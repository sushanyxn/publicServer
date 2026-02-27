package com.slg.net.crossevent.bridge;

import com.slg.common.event.manager.EventBusManager;
import com.slg.common.event.model.EventListenerWrapper;
import com.slg.common.event.model.IEvent;
import com.slg.common.log.LoggerUtil;
import com.slg.net.crossevent.ICrossServerEvent;
import com.slg.net.crossevent.model.CrossServerEventMeta;
import com.slg.net.crossevent.rpc.ICrossServerEventRpcService;
import com.slg.net.rpc.anno.RpcRef;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨服事件桥接器
 * 负责扫描、校验、注册和转发跨服事件
 *
 * <p>启动阶段：
 * <ol>
 *   <li>扫描所有 {@link ICrossServerEvent} 的具体实现类</li>
 *   <li>校验每个子类 toCrossEvent() 的协变返回类型</li>
 *   <li>预构建 VO 路由元数据并缓存</li>
 *   <li>为每个子类注册事件监听器（order=Integer.MAX_VALUE，优先级最低）</li>
 * </ol>
 *
 * <p>运行时：
 * <ol>
 *   <li>事件 publishEvent() 触发 Bridge 监听器</li>
 *   <li>检查 ThreadLocal 防重入标记，避免循环转发</li>
 *   <li>调用 toCrossEvent() 获取 VO</li>
 *   <li>根据缓存的路由元数据选择 RPC 方法并转发</li>
 * </ol>
 *
 * @author yangxunan
 * @date 2026/02/13
 */
@Component
public class CrossServerEventBridge {

    /**
     * ThreadLocal 防重入标记：接收远程事件时设为 true，阻止 Bridge 再次转发
     */
    private static final ThreadLocal<Boolean> RECEIVING = ThreadLocal.withInitial(() -> false);

    /**
     * 类路径扫描包
     */
    private static final String[] SCAN_PACKAGES = {"com.slg"};

    @Autowired
    private EventBusManager eventBusManager;

    @RpcRef
    private ICrossServerEventRpcService rpcService;

    /**
     * VO 类 -> 路由元数据（启动时一次性构建，运行时只读）
     */
    private final Map<Class<?>, CrossServerEventMeta> metaCache = new ConcurrentHashMap<>();

    /**
     * 供 RPC 实现类调用，设置防重入标记
     *
     * @param value true 表示正在接收远程事件，Bridge 应跳过转发
     */
    public static void setReceiving(boolean value) {
        RECEIVING.set(value);
    }

    /**
     * 启动初始化：扫描 -> 校验 -> 预构建元数据 -> 注册监听器
     * 校验不通过则抛异常，启动失败
     */
    @PostConstruct
    public void init() throws Exception {
        // 1. 扫描所有 ICrossServerEvent 的具体实现类
        Set<Class<? extends ICrossServerEvent>> eventClasses = scanCrossServerEvents();

        if (eventClasses.isEmpty()) {
            LoggerUtil.debug("[跨服事件] 未发现任何 ICrossServerEvent 实现类");
            return;
        }

        // 2. 获取转发处理方法
        Method forwardMethod = this.getClass().getDeclaredMethod("onCrossServerEvent", IEvent.class);

        // 3. 对每个跨服事件类：校验返回类型 + 预构建元数据 + 注册监听器
        for (Class<? extends ICrossServerEvent> eventClass : eventClasses) {
            // --- 校验 toCrossEvent() 协变返回类型 ---
            Class<?> voType = validateAndGetVoType(eventClass);

            // --- 预构建 VO 路由元数据（也做路由注解校验） ---
            CrossServerEventMeta meta = CrossServerEventMeta.resolve(voType);
            metaCache.put(voType, meta);

            // --- 注册事件监听器 ---
            EventListenerWrapper wrapper = new EventListenerWrapper(
                    this, forwardMethod, Integer.MAX_VALUE  // 最低优先级，本地监听器先执行
            );
            eventBusManager.registerListener(eventClass, wrapper);

            LoggerUtil.debug("[跨服事件] 注册转发监听: {} -> {}",
                    eventClass.getSimpleName(), voType.getSimpleName());
        }

        LoggerUtil.debug("[跨服事件] 初始化完成，共注册 {} 个跨服事件", eventClasses.size());
    }

    /**
     * 校验子类 toCrossEvent() 的协变返回类型
     * 必须是：非抽象具体类 + 位于 com.slg.net.message 包下
     *
     * @param eventClass ICrossServerEvent 子类
     * @return 协变返回类型（VO 类）
     * @throws IllegalStateException  校验不通过
     * @throws NoSuchMethodException  方法不存在
     */
    private Class<?> validateAndGetVoType(Class<?> eventClass) throws NoSuchMethodException {
        Method method = eventClass.getMethod("toCrossEvent");
        Class<?> returnType = method.getReturnType();

        // 校验 1：不允许返回 IEvent 原始类型（必须声明具体协变返回类型）
        if (returnType == IEvent.class || returnType == Object.class) {
            throw new IllegalStateException(
                    eventClass.getSimpleName() + " 的 toCrossEvent() 必须声明具体的协变返回类型，不能是 IEvent");
        }

        // 校验 2：返回类型必须位于 com.slg.net.message 包下
        if (!returnType.getPackageName().startsWith("com.slg.net.message")) {
            throw new IllegalStateException(
                    eventClass.getSimpleName() + " 的 toCrossEvent() 返回类型 " +
                            returnType.getName() + " 必须位于 com.slg.net.message 包下");
        }

        // 校验 3：返回类型必须是非抽象具体类
        if (returnType.isInterface() || Modifier.isAbstract(returnType.getModifiers())) {
            throw new IllegalStateException(
                    eventClass.getSimpleName() + " 的 toCrossEvent() 返回类型 " +
                            returnType.getSimpleName() + " 不能是接口或抽象类");
        }

        return returnType;
    }

    /**
     * 跨服事件转发处理方法
     * 被 EventListenerWrapper 通过 MethodHandle 调用
     *
     * @param event 事件对象
     */
    public void onCrossServerEvent(IEvent event) {
        // 防重入：RPC 接收端发布事件时跳过转发
        if (RECEIVING.get()) {
            return;
        }

        if (!(event instanceof ICrossServerEvent csEvent)) {
            return;
        }

        // 调用 toCrossEvent() 获取 VO
        IEvent vo = csEvent.toCrossEvent();
        if (vo == null) {
            // 动态决定不转发
            return;
        }

        // 启动时已预构建，直接取
        CrossServerEventMeta meta = metaCache.get(vo.getClass());
        if (meta == null) {
            LoggerUtil.error("[跨服事件] 未找到 VO 路由元数据: {}", vo.getClass().getSimpleName());
            return;
        }

        // 分发逻辑由 RouteType 内聚实现，无需 switch
        try {
            meta.dispatch(rpcService, vo);
        } catch (Exception e) {
            LoggerUtil.error("[跨服事件] 转发失败: event=" + event.getClass().getSimpleName() +
                    ", vo=" + vo.getClass().getSimpleName(), e);
        }
    }

    /**
     * 扫描 com.slg 包下所有 ICrossServerEvent 的具体实现类
     *
     * @return 所有具体实现类集合
     */
    @SuppressWarnings("unchecked")
    private Set<Class<? extends ICrossServerEvent>> scanCrossServerEvents() {
        Set<Class<? extends ICrossServerEvent>> result = new HashSet<>();

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(SCAN_PACKAGES)
                .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesImplementing(ICrossServerEvent.class)) {
                // 跳过接口和抽象类，只要具体实现类
                if (classInfo.isInterface() || classInfo.isAbstract()) {
                    continue;
                }

                Class<?> clazz = classInfo.loadClass();
                result.add((Class<? extends ICrossServerEvent>) clazz);
            }
        } catch (Exception e) {
            LoggerUtil.error("[跨服事件] 扫描 ICrossServerEvent 实现类异常", e);
            throw new RuntimeException("扫描跨服事件实现类失败", e);
        }

        return result;
    }
}
