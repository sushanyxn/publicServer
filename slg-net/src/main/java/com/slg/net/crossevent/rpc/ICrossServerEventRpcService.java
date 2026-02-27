package com.slg.net.crossevent.rpc;

import com.slg.common.executor.TaskModule;
import com.slg.net.rpc.anno.RpcMethod;
import com.slg.net.rpc.anno.RpcRouteParams;
import com.slg.net.rpc.anno.ThreadKey;
import com.slg.net.rpc.route.impl.PlayerCurrentSceneRoute;
import com.slg.net.rpc.route.impl.PlayerGameRoute;
import com.slg.net.rpc.route.impl.PlayerMainSceneRoute;
import com.slg.net.rpc.route.impl.ServerIdRoute;

/**
 * 跨服事件分发 RPC 接口
 * 定义 4 种路由策略的分发方法，分别对应 4 种路由注解
 *
 * <p>所有方法为 fire-and-forget 模式（void 返回值），跨服事件不需要等待结果
 * <p>eventVO 参数利用现有 MessageCodec 的运行时多态序列化，
 * 只要 VO 类注册在 message.yml 即可正确编解码
 *
 * @author yangxunan
 * @date 2026/02/13
 */
public interface ICrossServerEventRpcService {

    /**
     * 按服务器 ID 分发跨服事件
     *
     * @param serverId 目标服务器 ID
     * @param eventVO  事件 VO 对象
     */
    @RpcMethod(routeClz = ServerIdRoute.class, useModule = TaskModule.SYSTEM)
    void dispatchByServer(@RpcRouteParams int serverId, Object eventVO);

    /**
     * 按玩家所在 Game 服分发跨服事件
     *
     * @param playerId 玩家 ID（同时作为 ThreadKey）
     * @param eventVO  事件 VO 对象
     */
    @RpcMethod(routeClz = PlayerGameRoute.class, useModule = TaskModule.PLAYER)
    void dispatchByPlayerGame(@RpcRouteParams @ThreadKey long playerId, Object eventVO);

    /**
     * 按玩家主场景服分发跨服事件
     *
     * @param playerId 玩家 ID（同时作为 ThreadKey）
     * @param eventVO  事件 VO 对象
     */
    @RpcMethod(routeClz = PlayerMainSceneRoute.class, useModule = TaskModule.PLAYER)
    void dispatchByPlayerMainScene(@RpcRouteParams @ThreadKey long playerId, Object eventVO);

    /**
     * 按玩家当前场景服分发跨服事件
     *
     * @param playerId 玩家 ID（同时作为 ThreadKey）
     * @param eventVO  事件 VO 对象
     */
    @RpcMethod(routeClz = PlayerCurrentSceneRoute.class, useModule = TaskModule.PLAYER)
    void dispatchByPlayerCurrentScene(@RpcRouteParams @ThreadKey long playerId, Object eventVO);
}
