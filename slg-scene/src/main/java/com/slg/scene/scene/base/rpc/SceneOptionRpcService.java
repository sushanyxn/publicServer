package com.slg.scene.scene.base.rpc;

import com.slg.net.message.clientmessage.scene.packet.ScenePlayerVO;
import com.slg.net.rpc.impl.scene.ISceneOptionRpcService;
import com.slg.scene.base.manager.ScenePlayerManager;
import com.slg.scene.scene.base.service.SceneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 场景RPC服务提供方
 *
 * @author yangxunan
 * @date 2026/1/23
 */
@Component
public class SceneOptionRpcService implements ISceneOptionRpcService {

    /**
     * 业务Service
     */
    @Autowired
    private SceneService sceneService;

    @Autowired
    private ScenePlayerManager scenePlayerManager;


    @Override
    public CompletableFuture<Integer> verifyEnterScene(int serverId, long playerId, int sceneId){
        return CompletableFuture.completedFuture(sceneService.verifyEnterScene(playerId, sceneId));
    }

    @Override
    public CompletableFuture<Integer> enterScene(int serverId, long playerId, int sceneId){
        // 返回业务结果
        return CompletableFuture.completedFuture(sceneService.enterScene(playerId, sceneId));
    }

    @Override
    public void exitScene(long player, int sceneId){
        sceneService.exitScene(player, sceneId);
    }

    @Override
    public void bindPlayer(int serverId, long playerId, ScenePlayerVO scenePlayerVO){

    }

    @Override
    public void watch(long playerId, int x, int y, int layer){
        sceneService.watchScene(playerId, x, y, layer);
    }
}
