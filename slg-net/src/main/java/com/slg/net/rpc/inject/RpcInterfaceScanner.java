package com.slg.net.rpc.inject;

import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.anno.RpcMethod;
import com.slg.net.rpc.anno.RpcRouteParams;
import com.slg.net.rpc.anno.ThreadKey;
import com.slg.net.rpc.manager.RpcProxyManager;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.route.AbstractRpcRoute;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RPC 接口扫描器
 * 在 Spring 容器启动时自动扫描所有 RPC 接口，生成基础元数据
 * 解决纯客户端（没有实现类）无法注册元数据的问题
 *
 * <p>注意事项：
 * <ul>
 *   <li>只注册基础元数据（bean=null, methodHandle=null, executor=null）</li>
 *   <li>线程池只在有实现类时才注入（由 RpcMethodInjector 处理）</li>
 *   <li>纯客户端不需要线程池，只需要代理对象</li>
 * </ul>
 *
 * <p>互补扫描机制：
 * <ul>
 *   <li>RpcInterfaceScanner 在 afterPropertiesSet 时扫描所有接口（自动执行）</li>
 *   <li>RpcMethodInjector 在处理 Bean 时检查是否已注册，未注册则执行完整注册</li>
 *   <li>两者执行顺序不固定，但能相互补充，确保所有 RPC 方法都能正确注册</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class RpcInterfaceScanner implements InitializingBean {

    @Autowired
    private RpcProxyManager rpcProxyManager;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * RPC 接口扫描包路径
     */
    private static final String[] SCAN_PACKAGES = {
            "com.slg.net.rpc.impl"
    };

    /**
     * 在属性设置完成后立即执行扫描
     * 自动由 Spring 调用，不需要手动触发
     */
    @Override
    public void afterPropertiesSet() {
        LoggerUtil.debug("[RPC] 开始扫描 RPC 接口，包路径: {}",
                String.join(", ", SCAN_PACKAGES));
        scanRpcInterfaces();
        LoggerUtil.debug("[RPC] RPC 接口扫描完成，共注册 {} 个方法（无实现类）",
                rpcProxyManager.getMethodCount());
    }

    /**
     * 扫描所有 RPC 接口
     */
    private void scanRpcInterfaces() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableMethodInfo()
                .enableAnnotationInfo()
                .acceptPackages(SCAN_PACKAGES)
                .scan()) {

            // 获取所有接口
            for (ClassInfo classInfo : scanResult.getAllInterfaces()) {
                Class<?> interfaceClass = classInfo.loadClass();

                // 遍历接口的所有方法
                for (Method method : interfaceClass.getDeclaredMethods()) {
                    RpcMethod rpcMethod = method.getAnnotation(RpcMethod.class);
                    if (rpcMethod == null) {
                        continue;
                    }

                    // 构建方法标识
                    String methodMarker = interfaceClass.getName() + "#" + method.getName();

                    try {
                        // 检查是否已被 RpcMethodInjector 完整注册
                        RpcMethodMeta existingMeta = rpcProxyManager.getRpcMethodMeta(methodMarker);
                        if (existingMeta != null) {
                            // 已注册（RpcMethodInjector 先执行），跳过以保留实现信息
                            continue;
                        }

                        // 验证返回值类型（必须是 void 或 CompletableFuture）
                        validateReturnType(method, methodMarker);

                        // 构建方法元数据（无实现类）
                        RpcMethodMeta meta = buildMethodMetaWithoutImpl(
                                interfaceClass, method, rpcMethod);

                        // 验证路由参数定义
                        if (!meta.getRouteInstance().verifyRouteParamsDefine(meta)) {
                            LoggerUtil.error("[RPC] 路由参数定义不正确: {}", methodMarker);
                            throw new IllegalStateException("路由参数定义不正确: " + methodMarker);
                        }

                        // 注册到管理器
                        rpcProxyManager.registerMethodMeta(methodMarker, meta);

                    } catch (Exception e) {
                        LoggerUtil.error("[RPC] 注册接口方法失败: {}", methodMarker, e);
                        throw new RuntimeException("注册 RPC 接口方法失败: " + methodMarker, e);
                    }
                }
            }

        } catch (Exception e) {
            LoggerUtil.error("[RPC] RPC 接口扫描异常", e);
            throw new RuntimeException("RPC 接口扫描失败", e);
        }
    }

    /**
     * 构建 RPC 方法元数据（无实现类版本）
     * 用于纯客户端场景，bean、methodHandle 和 executor 均为 null
     *
     * <p>线程池只在服务端（有实现类）时才需要，纯客户端不需要
     *
     * @param iface     接口类
     * @param method    方法
     * @param rpcMethod RPC 方法注解
     * @return 方法元数据（无实现类信息）
     */
    private RpcMethodMeta buildMethodMetaWithoutImpl(Class<?> iface, Method method,
                                                      RpcMethod rpcMethod) {
        RpcMethodMeta meta = new RpcMethodMeta();

        // 设置基本信息
        meta.setMethodMarker(iface.getName() + "#" + method.getName());
        meta.setMethod(method);
        meta.setReturnType(method.getReturnType());
        meta.setRouteClass(rpcMethod.routeClz());
        meta.setBean(null);  // 暂时为空，等待实现类更新
        meta.setMethodHandle(null);  // 暂时为空，等待实现类更新

        // 解析参数注解
        Parameter[] parameters = method.getParameters();
        List<Integer> routeParamsList = new ArrayList<>();

        for (int i = 0; i < parameters.length; i++) {
            // 查找 @RpcRouteParams 注解
            if (parameters[i].isAnnotationPresent(RpcRouteParams.class)) {
                routeParamsList.add(i);
            }
            // 查找 @ThreadKey 注解
            if (parameters[i].isAnnotationPresent(ThreadKey.class)) {
                meta.setThreadKeyIndex(i);
            }
        }

        // 设置路由参数下标数组
        meta.setRouteParamsIndex(routeParamsList.stream().mapToInt(i -> i).toArray());

        // 获取路由实例（从 Spring 容器）
        AbstractRpcRoute routeInstance = applicationContext.getBean(rpcMethod.routeClz());
        meta.setRouteInstance(routeInstance);

        // 设置超时时间
        meta.setTimeoutMillis(rpcMethod.timeoutMillis());

        // 注意：线程池不在接口扫描阶段注入，只在有实现类时才注入
        // 纯客户端不需要线程池

        return meta;
    }

    /**
     * 验证 RPC 方法返回值类型
     * 强制要求返回值只能是 void 或 CompletableFuture
     *
     * @param method       方法对象
     * @param methodMarker 方法标识
     * @throws IllegalStateException 如果返回值类型不合法
     */
    private void validateReturnType(Method method, String methodMarker) {
        Class<?> returnType = method.getReturnType();

        // 允许 void 或 CompletableFuture
        if (returnType == Void.TYPE || CompletableFuture.class.isAssignableFrom(returnType)) {
            return;
        }

        // 不允许其他类型
        String errorMsg = String.format(
                "[RPC] 非法的返回值类型！方法: %s，当前返回值: %s。" +
                "RPC 方法的返回值必须是 void 或 CompletableFuture<T>，以保证异步调用的一致性。",
                methodMarker, returnType.getName());

        LoggerUtil.error(errorMsg);
        throw new IllegalStateException(errorMsg);
    }

}

