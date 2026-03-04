package com.slg.net.rpc.impl.scene;

import com.slg.net.message.clientmessage.hero.packet.HeroVO;
import com.slg.net.message.clientmessage.scene.packet.ScenePlayerVO;
import com.slg.net.rpc.anno.RpcMethod;
import com.slg.net.rpc.anno.RpcRouteParams;
import com.slg.net.rpc.anno.ThreadKey;
import com.slg.net.rpc.route.impl.PlayerCurrentSceneRoute;
import com.slg.net.rpc.route.impl.RedisRoute;

import java.util.concurrent.CompletableFuture;

/**
 * 场景操作相关 RPC
 *
 * @author yangxunan
 * @date 2026/01/23
 */
public interface ISceneOptionRpcService {

    /**
     * 验证是否可以进入场景
     * @param serverId 目标服务器Id
     * @param playerId 玩家id
     * @param sceneId 场景Id
     * @return 进入场景的结果
     */
    @RpcMethod
    CompletableFuture<Integer> verifyEnterScene(@RpcRouteParams int serverId, @ThreadKey long playerId, int sceneId);

    /**
     * 玩家进入场景
     * 默认使用服务器id路由
     *
     * @param serverId 目标服务器Id
     * @param playerId 玩家id
     * @param sceneId 场景Id
     * @return 进入场景的结果
     */
    @RpcMethod
    CompletableFuture<Integer> enterScene(@RpcRouteParams int serverId, @ThreadKey long playerId, int sceneId);

    /**
     * 玩家退出当前场景
     * @param player
     * @param sceneId
     */
    @RpcMethod(routeClz =  PlayerCurrentSceneRoute.class)
    void exitScene(@RpcRouteParams @ThreadKey long player, int sceneId);

    /**
     * 场景服绑定玩家
     * @param serverId
     * @param playerId
     * @param scenePlayerVO
     */
    @RpcMethod
    void bindPlayer(@RpcRouteParams int serverId, @ThreadKey long playerId, ScenePlayerVO scenePlayerVO);

    /**
     * 在场景中移动镜头
     * 使用玩家当前场景路由
     *
     * @param playerId 玩家ID
     * @param x        坐标 X
     * @param y        坐标 Y
     * @param layer    层级
     */
    @RpcMethod(routeClz = PlayerCurrentSceneRoute.class)
    void watch(@RpcRouteParams @ThreadKey long playerId, int x, int y, int layer);


}
