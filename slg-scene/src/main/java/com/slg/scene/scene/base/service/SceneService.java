package com.slg.scene.scene.base.service;

import com.slg.scene.base.manager.ScenePlayerManager;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.base.model.WatchPlayer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.base.handler.AbstractSceneHandler;
import com.slg.scene.scene.base.manager.SceneManager;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.base.table.SceneTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 场景服务
 *
 * @author yangxunan
 * @date 2026/1/27
 */
@Component
public class SceneService {

    @Autowired
    private ScenePlayerManager scenePlayerManager;
    @Autowired
    private SceneManager sceneManager;

    public int verifyEnterScene(long playerId, int sceneConfigId){

        ScenePlayer scenePlayer = scenePlayerManager.getScenePlayer(playerId);
        if (scenePlayer == null) {
            // 创建临时watchPlayer
            scenePlayer = scenePlayerManager.createTempWatchPlayer(playerId);
        }
        SceneTable sceneTable = sceneManager.getSceneTable(sceneConfigId);
        AbstractSceneHandler sceneHandler = AbstractSceneHandler.getHandler(sceneTable.getType());
        int result = sceneHandler.verifyEnter(scenePlayer, sceneConfigId);

        /**
         * 返回前需要先退出现在的场景
         * 防止客户端在加载新场景时，旧的场景还在同步AOI消息
         */
        if (result == 0 && scenePlayer.getSceneConfigId() > 0) {
            exitScene(scenePlayer.getId(), sceneConfigId);
        }

        return result;
    }

    public int enterScene(long playerId, int sceneConfigId){

        ScenePlayer scenePlayer = scenePlayerManager.getScenePlayer(playerId);

        SceneTable sceneTable = sceneManager.getSceneTable(sceneConfigId);
        AbstractSceneHandler sceneHandler = AbstractSceneHandler.getHandler(sceneTable.getType());
        int result = sceneHandler.enterScene(scenePlayer, sceneConfigId);
        if (result == 0) {
            scenePlayer.setSceneConfigId(sceneConfigId);
        }

        return result;
    }

    public void exitScene(long playerId, int sceneConfigId){

        ScenePlayer scenePlayer = scenePlayerManager.getScenePlayer(playerId);
        SceneTable sceneTable = sceneManager.getSceneTable(sceneConfigId);
        AbstractSceneHandler sceneHandler = AbstractSceneHandler.getHandler(sceneTable.getType());
        if (sceneHandler != null) {
            sceneHandler.exitScene(scenePlayer, sceneConfigId);
        }
        if (scenePlayer instanceof WatchPlayer watchPlayer && watchPlayer.getSceneConfigId() == sceneConfigId) {
            // 观察者player需要跟随player退出场景一起移除
            scenePlayerManager.getWatchPlayers().remove(watchPlayer.getId());
        }
    }

    public void watchScene(long playerId, int x, int y, int layer){

        ScenePlayer scenePlayer = scenePlayerManager.getScenePlayer(playerId);
        if (scenePlayer == null) {
            return;
        }
        Scene scene = scenePlayer.getScene();
        if (scene == null) {
            return;
        }
        scene.getAoiController().submitWatchTask(playerId, Position.valueOf(scene, x, y), GridLayer.getGridLayer(layer));

    }

}
