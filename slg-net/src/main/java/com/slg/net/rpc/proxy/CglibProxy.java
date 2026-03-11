package com.slg.net.rpc.proxy;

import com.slg.common.executor.core.KeyedVirtualExecutor;
import com.slg.common.executor.TaskModule;
import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.exception.RpcException;
import com.slg.net.rpc.manager.RpcProxyManager;
import com.slg.net.rpc.manager.RpcRemoteManager;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.util.RpcThreadUtil;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * RPC CGLIB 动态代理
 * 使用 CGLIB 创建 RPC 接口的代理对象，拦截方法调用并路由到本地或远程
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class CglibProxy {

    private final RpcProxyManager rpcProxyManager;
    private final RpcRemoteManager rpcRemoteManager;

    public CglibProxy(
            @Lazy RpcProxyManager rpcProxyManager,
            @Lazy RpcRemoteManager rpcRemoteManager) {
        this.rpcProxyManager = rpcProxyManager;
        this.rpcRemoteManager = rpcRemoteManager;
    }

    /**
     * 创建代理对象
     *
     * @param interfaceClass 接口类
     * @param <T>            接口类型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass) {
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[]{interfaceClass});
        enhancer.setCallback(new RpcMethodInterceptor());
        return (T) enhancer.create();
    }

    /**
     * RPC 方法拦截器
     * 拦截所有方法调用，根据路由判断是本地调用还是远程调用
     */
    private class RpcMethodInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            // 构建方法标识
            String methodMarker = method.getDeclaringClass().getName() + "#" + method.getName();

            // 查找方法元数据
            RpcMethodMeta meta = rpcProxyManager.getRpcMethodMeta(methodMarker);
            if (meta == null) {
                LoggerUtil.error("[RPC] 方法未注册: {}", methodMarker);
                throw new RpcException("RPC 方法未注册: " + methodMarker);
            }

            // 判断是否本地调用
            if (meta.getRouteInstance().isLocal(meta, args)) {
                // 本地调用：需要判断线程
                return invokeLocal(meta, args, methodMarker);
            } else {
                // 远程调用：委托给 RpcRemoteManager
                return rpcRemoteManager.invokeRemote(meta, args);
            }
        }

        /**
         * 执行本地调用（包含线程切换逻辑）
         *
         * @param meta         方法元数据
         * @param args         方法参数
         * @param methodMarker 方法标识
         * @return 调用结果
         * @throws Throwable 调用异常
         */
        private Object invokeLocal(RpcMethodMeta meta, Object[] args, String methodMarker) throws Throwable {
            // 检查是否有实现类
            if (meta.getBean() == null || meta.getMethodHandle() == null) {
                LoggerUtil.error("[RPC] 本地调用失败，没有实现类: {}", methodMarker);
                throw new RpcException("本地调用失败，没有实现类: " + methodMarker);
            }
            
            // 检查是否配置了任务模块
            TaskModule taskModule = meta.getTaskModule();
            if (taskModule == null) {
                LoggerUtil.error("[RPC] 本地调用失败，未配置任务模块: {}", methodMarker);
                throw new RpcException("本地调用失败，未配置任务模块: " + methodMarker);
            }

            // 判断是否已在目标虚拟线程链中
            KeyedVirtualExecutor executor = KeyedVirtualExecutor.getInstance();
            boolean multiChain = taskModule.isMultiChain();
            long threadKey = multiChain ? RpcThreadUtil.extractThreadKey(meta, args) : 0;
            boolean inThread = multiChain ? executor.inThread(taskModule, threadKey) : executor.inThread(taskModule);

            if (inThread) {
                // 当前就是目标线程，同步执行
                return invokeMethod(meta, args, methodMarker);
            } else {
                // 需要切换线程
                CompletableFuture<Object> future = new CompletableFuture<>();
                Runnable wrappedTask = () -> {
                    try {
                        Object result = invokeMethod(meta, args, methodMarker);
                        // 如果返回值本身是 CompletableFuture，需要等待它完成
                        if (result instanceof CompletableFuture) {
                            ((CompletableFuture<?>) result).whenComplete((r, ex) -> {
                                if (ex != null) {
                                    future.completeExceptionally(ex);
                                } else {
                                    future.complete(r);
                                }
                            });
                        } else {
                            future.complete(result);
                        }
                    } catch (Throwable e) {
                        LoggerUtil.error("[RPC] 本地调用异常: {}", methodMarker, e);
                        future.completeExceptionally(new RpcException("本地调用异常: " + methodMarker, e));
                    }
                };
                if (multiChain) {
                    executor.execute(taskModule, threadKey, wrappedTask);
                } else {
                    executor.execute(taskModule, wrappedTask);
                }
                return future;
            }
        }

        /**
         * 执行方法调用
         *
         * @param meta         方法元数据
         * @param args         方法参数
         * @param methodMarker 方法标识
         * @return 调用结果
         * @throws Throwable 调用异常
         */
        private Object invokeMethod(RpcMethodMeta meta, Object[] args, String methodMarker) throws Throwable {
            try {
                return meta.getMethodHandle().invoke(meta.getBean(), args);
            } catch (Throwable e) {
                LoggerUtil.error("[RPC] 方法调用异常: {}", methodMarker, e);
                throw new RpcException("方法调用异常: " + methodMarker, e);
            }
        }
    }

}

