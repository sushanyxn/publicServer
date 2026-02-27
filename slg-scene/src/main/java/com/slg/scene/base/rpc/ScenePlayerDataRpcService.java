package com.slg.scene.base.rpc;

import com.slg.common.constant.OptionResult;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.clientmessage.scene.packet.ScenePlayerVO;
import com.slg.net.rpc.impl.scene.IScenePlayerDataRpcService;
import com.slg.scene.base.manager.ScenePlayerManager;
import com.slg.scene.base.model.ScenePlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import static com.slg.common.constant.OptionResult.SCENE_PLAYER_NOT_FOUND;
import static com.slg.common.constant.OptionResult.SUCCESS;

/**
 * @author yangxunan
 * @date 2026/2/11
 */
@Component
public class ScenePlayerDataRpcService implements IScenePlayerDataRpcService {

    @Autowired
    private ScenePlayerManager scenePlayerManager;

    @Override
    public CompletableFuture<Integer> createScenePlayer(long playerId, ScenePlayerVO scenePlayerVO){
        ScenePlayer scenePlayer = scenePlayerManager.getScenePlayer(scenePlayerVO.getPlayerId());
        if (scenePlayer != null) {
            LoggerUtil.error("场景服务创建ScenePlayer时, 玩家{}已经存在", scenePlayer.getId());
            scenePlayer.getScenePlayerEntity().init(scenePlayerVO);
        } else{
            scenePlayerManager.create(scenePlayerVO);
        }

        return CompletableFuture.completedFuture(SUCCESS);
    }

    @Override
    public CompletableFuture<Integer> bindScenePlayer(long playerId){

        ScenePlayer scenePlayer = scenePlayerManager.getScenePlayer(playerId);
        int result = scenePlayer == null ? SCENE_PLAYER_NOT_FOUND : SUCCESS;

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Integer> initPlayer(long playerId) {
        ScenePlayer scenePlayer = scenePlayerManager.getScenePlayer(playerId);
        if (scenePlayer == null) {
            return CompletableFuture.completedFuture(SCENE_PLAYER_NOT_FOUND);
        }
        if (scenePlayer.isSceneInited()) {
            return CompletableFuture.completedFuture(SUCCESS); // 已初始化，幂等返回
        }

        // 执行节点创建（主城等）
        // TODO: 具体的场景节点创建逻辑

        scenePlayer.setSceneInited(true);
        return CompletableFuture.completedFuture(SUCCESS);
    }
}
