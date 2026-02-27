package com.slg.scene.scene.base.handler;

import com.slg.common.constant.SceneType;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.aoi.model.Watcher;
import com.slg.scene.scene.base.manager.SceneManager;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.base.model.Scene;
import org.springframework.stereotype.Component;

/**
 * 大地图handler
 *
 * @author yangxunan
 * @date 2026/2/2
 */
@Component
public class MainSceneHandler extends AbstractSceneHandler {

    @Override
    public SceneType getSceneType(){
        return SceneType.MAIN_SCENE;
    }

    @Override
    public int verifyEnter(ScenePlayer player, int sceneId){
        // 主场景不需要验证什么
        return 0;
    }

    @Override
    public int enterScene(ScenePlayer player, int sceneId){

        Scene scene = getScene(player, sceneId);

        Watcher watcher = new Watcher(player);
        watcher.setGridLayer(GridLayer.DETAIL);
        watcher.setPosition(getEnterInitPosition(player, scene));
        scene.enter(watcher);
        return 0;
    }

    @Override
    public void exitScene(ScenePlayer player, int sceneId){

        Scene scene = getScene(player, sceneId);
        scene.exit(player.getId());
    }

    @Override
    public Position getEnterInitPosition(ScenePlayer player, Scene scene){

        if (player.getMainCity() != null) {
            // 有主城，就看主城
            return player.getMainCity().getPosition();
        }

        // 没有主城的话，一般看着王城
        return Position.valueOf(scene, 300, 300);
    }

    @Override
    public Scene getScene(ScenePlayer player, int sceneConfigId){
        // 主场景 只有固定的一个场景对象，直接拿就可以了
        return SceneManager.getMainScene();
    }
}
