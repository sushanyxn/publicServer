package com.slg.net.rpc.inject;

import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.anno.RpcMethod;
import com.slg.net.rpc.anno.RpcRouteParams;
import com.slg.net.rpc.anno.ThreadKey;
import com.slg.net.rpc.manager.RpcProxyManager;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.route.AbstractRpcRoute;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RPC 方法实现类扫描器
 * 在 Spring Bean 初始化后，为已注册的 RPC 方法更新实现类信息
 * 配合 RpcInterfaceScanner 使用：
 * 1. RpcInterfaceScanner 扫描接口，注册基础元数据（如果它先执行）
 * 2. RpcMethodInjector 扫描实现类，更新 bean 和 methodHandle
 *
 * <p>互补扫描机制：
 * <ul>
 *   <li>如果 RpcInterfaceScanner 先执行：直接更新已注册的元数据</li>
 *   <li>如果 RpcMethodInjector 先执行：执行完整注册（包括接口信息）</li>
 *   <li>两者执行顺序不固定，但通过互补机制保证所有方法都能正确注册</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class RpcMethodInjector implements BeanPostProcessor {

    private final RpcProxyManager rpcProxyManager;
    private final ApplicationContext applicationContext;

    /**
     * 构造函数
     *
     * @param rpcProxyManager    RPC 代理管理器（@Lazy 避免循环依赖）
     * @param applicationContext Spring 应用上下文
     */
    public RpcMethodInjector(
            @Lazy RpcProxyManager rpcProxyManager,
            ApplicationContext applicationContext) {
        this.rpcProxyManager = rpcProxyManager;
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {

        Class<?> beanClass = bean.getClass();

        // 遍历 Bean 实现的所有接口
        for (Class<?> iface : beanClass.getInterfaces()) {
            // 遍历接口的所有方法
            for (Method method : iface.getDeclaredMethods()) {
                RpcMethod rpcMethod = method.getAnnotation(RpcMethod.class);
                if (rpcMethod == null) {
                    // 方法未标注 @RpcMethod，跳过
                    continue;
                }

                // 构建方法标识
                String methodMarker = iface.getName() + "#" + method.getName();

                // 验证返回值类型（双重验证，防止接口扫描被绕过）
                validateReturnType(method, methodMarker);

                // 检查方法是否已被 RpcInterfaceScanner 注册
                RpcMethodMeta existingMeta = rpcProxyManager.getRpcMethodMeta(methodMarker);

                if (existingMeta != null) {
                    // 方法已由 RpcInterfaceScanner 注册，仅更新实现类信息
                    try {
                        updateMethodImplementation(existingMeta, method, bean, methodMarker);
                    } catch (Exception e) {
                        LoggerUtil.error("[RPC] 更新方法实现失败: {}", methodMarker, e);
                        throw new RuntimeException("更新 RPC 方法实现失败: " + methodMarker, e);
                    }
                } else {
                    // 方法未注册（RpcInterfaceScanner 还未执行或执行较晚），执行完整注册
                    try {
                        RpcMethodMeta meta = buildMethodMeta(iface, method, rpcMethod, bean);
                        if (!meta.getRouteInstance().verifyRouteParamsDefine(meta)) {
                            LoggerUtil.error("[RPC] 路由参数定义不正确: {}", methodMarker);
                            throw new IllegalStateException("路由参数定义不正确: " + methodMarker);
                        }
                        rpcProxyManager.registerMethodMeta(methodMarker, meta);
                    } catch (Exception e) {
                        LoggerUtil.error("[RPC] 注册方法失败: {}", methodMarker, e);
                        throw new RuntimeException("注册 RPC 方法失败: " + methodMarker, e);
                    }
                }
            }
        }

        return bean;
    }

    /**
     * 更新方法实现类信息
     *
     * @param meta         已存在的方法元数据
     * @param method       方法对象
     * @param bean         实现类 Bean
     * @param methodMarker 方法标识
     */
    private void updateMethodImplementation(RpcMethodMeta meta, Method method,
                                            Object bean, String methodMarker) throws Exception {
        // 创建 MethodHandle（高性能反射调用）
        // 使用 asSpreader 将参数部分转为 Object[] 展开形式，
        // 使得调用时可以 invoke(bean, Object[] args) 自动展开为各个独立参数
        MethodHandle methodHandle = MethodHandles.lookup().unreflect(method);
        methodHandle = methodHandle.asSpreader(Object[].class, method.getParameterCount());

        // 更新实现类信息
        meta.setBean(bean);
        meta.setMethodHandle(methodHandle);

        // 直接存储 TaskModule（零依赖，不需要查找线程池）
        RpcMethod rpcMethod = method.getAnnotation(RpcMethod.class);
        if (rpcMethod != null) {
            meta.setTaskModule(rpcMethod.useModule());
        }
    }

    /**
     * 构建 RPC 方法元数据
     */
    private RpcMethodMeta buildMethodMeta(Class<?> iface, Method method,
                                          RpcMethod rpcMethod, Object bean) throws Exception {
        RpcMethodMeta meta = new RpcMethodMeta();

        // 设置基本信息
        meta.setMethodMarker(iface.getName() + "#" + method.getName());
        meta.setMethod(method);
        meta.setReturnType(method.getReturnType());
        meta.setRouteClass(rpcMethod.routeClz());
        meta.setBean(bean);

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

        // 创建 MethodHandle（高性能反射调用）
        // 使用 asSpreader 将参数部分转为 Object[] 展开形式，
        // 使得调用时可以 invoke(bean, Object[] args) 自动展开为各个独立参数
        MethodHandle methodHandle = MethodHandles.lookup().unreflect(method);
        methodHandle = methodHandle.asSpreader(Object[].class, method.getParameterCount());
        meta.setMethodHandle(methodHandle);

        // 获取路由实例（从 Spring 容器）
        AbstractRpcRoute routeInstance = applicationContext.getBean(rpcMethod.routeClz());
        meta.setRouteInstance(routeInstance);

        // 设置超时时间
        meta.setTimeoutMillis(rpcMethod.timeoutMillis());

        // 直接存储 TaskModule（零依赖，不需要查找线程池）
        meta.setTaskModule(rpcMethod.useModule());

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
