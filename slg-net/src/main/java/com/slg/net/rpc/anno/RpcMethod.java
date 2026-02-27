package com.slg.net.rpc.anno;

import com.slg.common.executor.TaskModule;
import com.slg.net.rpc.route.AbstractRpcRoute;
import com.slg.net.rpc.route.impl.ServerIdRoute;

import java.lang.annotation.*;

/**
 * RPC 方法注解
 * 标注在接口方法上，表示该方法支持 RPC 远程调用
 *
 * <p>该注解会被 RPC 框架扫描并生成元数据，支持：
 * <ul>
 *   <li>自动路由：根据路由参数判断本地执行或远程调用</li>
 *   <li>异步调用：支持 CompletableFuture 返回值</li>
 *   <li>超时控制：基于 Deadline 的超时机制</li>
 *   <li>线程分派：根据 ThreadKey 分派到不同线程（未来实现）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * public interface ISceneRpcService {
 *     // 无返回值（单向调用）
 *     @RpcMethod
 *     void watch(@RpcRouteParams int serverId, @ThreadKey long playerId, int x, int y);
 *
 *     // 有返回值（异步调用）
 *     @RpcMethod
 *     CompletableFuture<String> getInfo(@RpcRouteParams int serverId);
 *
 *     // 自定义路由策略
 *     @RpcMethod(routeClz = PlayerIdRoute.class)
 *     void notify(@RpcRouteParams long playerId, String message);
 * }
 * }
 * </pre>
 *
 * <p>方法签名要求：
 * <ul>
 *   <li>必须在接口中定义</li>
 *   <li>必须有至少一个 @RpcRouteParams 标注的参数</li>
 *   <li>路由参数类型必须匹配路由策略的要求</li>
 *   <li>返回值类型：void、CompletableFuture&lt;T&gt; 或其他可序列化类型</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/01/23
 * @see RpcRouteParams
 * @see ThreadKey
 * @see com.slg.net.rpc.inject.RpcInterfaceScanner
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcMethod {

    /**
     * 路由策略类
     * 指定使用哪个路由策略来判断调用目标
     *
     * <p>内置路由策略：
     * <ul>
     *   <li>{@link ServerIdRoute}（默认）：根据服务器 ID 路由</li>
     * </ul>
     *
     * @return 路由策略类
     */
    Class<? extends AbstractRpcRoute> routeClz() default ServerIdRoute.class;

    /**
     * 使用的任务模块
     * 用于指定该方法在哪个模块的虚拟线程链中执行
     */
    TaskModule useModule() default TaskModule.PLAYER;

    /**
     * RPC 调用超时时间（毫秒）
     * 超过此时间未返回结果，将触发超时异常
     * 
     * @return 超时时间，单位：毫秒，默认 30000（30秒）
     */
    long timeoutMillis() default 30000L;

}
