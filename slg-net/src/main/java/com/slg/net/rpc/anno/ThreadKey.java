package com.slg.net.rpc.anno;

import java.lang.annotation.*;

/**
 * 线程分派键注解
 * 标注在方法参数上，表示该参数用于线程分派决策
 *
 * <p>在多线程环境下，为了避免并发问题（如同一玩家的多个请求并发执行），
 * 可以根据 ThreadKey 参数将请求分派到不同的线程池执行。
 * 相同 ThreadKey 的请求会串行执行，不同 ThreadKey 的请求可以并行执行。
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * public interface ISceneRpcService {
 *     @RpcMethod
 *     void watch(@RpcRouteParams int serverId, @ThreadKey long playerId, int x, int y);
 *     //                                        ^^^^^^^^^^
 *     //                                        playerId 用于线程分派
 * }
 * }
 * </pre>
 *
 * <p>典型场景：
 * <ul>
 *   <li>按玩家 ID 分派：同一玩家的请求串行执行，避免数据竞争</li>
 *   <li>按场景 ID 分派：同一场景的请求串行执行</li>
 *   <li>按公会 ID 分派：同一公会的请求串行执行</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/01/23
 * @see RpcMethod#useModule()
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThreadKey {
}
