package com.slg.net.rpc.impl.scene;

import com.slg.net.message.clientmessage.scene.packet.ScenePlayerVO;
import com.slg.net.rpc.anno.RpcMethod;
import com.slg.net.rpc.anno.RpcRouteParams;
import com.slg.net.rpc.anno.ThreadKey;
import com.slg.net.rpc.route.impl.PlayerMainSceneRoute;

import java.util.concurrent.CompletableFuture;

/**
 * 玩家数据RPC
 *
 * @author yangxunan
 * @date 2026/2/10
 */
public interface IScenePlayerDataRpcService {

    /**
     * 创建玩家的ScenePlayer对象 一般用于创角时，和匹配新场景时
     * @param playerId
     * @param scenePlayerVO
     * @return
     */
    @RpcMethod(routeClz = PlayerMainSceneRoute.class)
    CompletableFuture<Integer> createScenePlayer(@RpcRouteParams @ThreadKey long playerId, ScenePlayerVO scenePlayerVO);

    /**
     * 加载玩家的ScenePlayer 一般用于起服加载时
     * @param playerId
     * @return
     */
    @RpcMethod(routeClz = PlayerMainSceneRoute.class)
    CompletableFuture<Integer> bindScenePlayer(@RpcRouteParams @ThreadKey long playerId);


    /**
     * 初始化玩家的场景单位（主城、军队等节点创建）
     *
     * @param playerId 玩家ID
     * @return 操作结果
     */
    @RpcMethod(routeClz = PlayerMainSceneRoute.class)
    CompletableFuture<Integer> initPlayer(@RpcRouteParams @ThreadKey long playerId);

}
