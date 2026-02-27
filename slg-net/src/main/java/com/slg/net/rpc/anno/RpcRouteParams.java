package com.slg.net.rpc.anno;

import java.lang.annotation.*;

/**
 * RPC 路由参数注解
 * 标注在方法参数上，表示该参数用于路由决策
 *
 * <p>路由参数会被传递给路由策略（如 ServerIdRoute），
 * 用于判断应该路由到哪个目标服务器或本地执行。
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * public interface ISceneRpcService {
 *     @RpcMethod
 *     void watch(@RpcRouteParams int serverId, long playerId, int x, int y);
 *     //         ^^^^^^^^^^^^^^^^
 *     //         serverId 参数用于路由判断
 * }
 * }
 * </pre>
 *
 * <p>不同的路由策略可能需要不同类型的路由参数：
 * <ul>
 *   <li>ServerIdRoute：需要 int 类型的 serverId</li>
 *   <li>PlayerIdRoute：需要 long 类型的 playerId</li>
 *   <li>SceneIdRoute：需要 int 类型的 sceneId</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/01/23
 * @see com.slg.net.rpc.route.AbstractRpcRoute#verifyRouteParamsDefine
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcRouteParams {
}
