package com.slg.game.scene.handler.impl;

import com.slg.common.constant.SceneType;
import com.slg.game.base.player.model.Player;
import com.slg.game.scene.handler.RealSceneHandler;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/2/2
 */
@Component("gameMainSceneHandler")
public class MainSceneHandler extends RealSceneHandler {
    @Override
    public int getSceneServerId(Player player, int sceneId){
        return player.getPlayerEntity().getSceneServerId();
    }

    @Override
    public boolean verifyEnterLocal(Player player, int sceneId){
        return true;
    }

    @Override
    public void beofreEnterScene(Player player, int sceneServerId, int sceneId){

    }

    @Override
    public SceneType getSceneType(){
        return SceneType.MAIN_SCENE;
    }

    @Override
    public void exitScene(Player player, int sceneServerId, int sceneId){

    }
}
