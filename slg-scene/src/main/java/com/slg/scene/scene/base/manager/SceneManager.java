package com.slg.scene.scene.base.manager;

import com.slg.common.constant.SceneType;
import com.slg.scene.base.model.SceneIdCreate;
import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.base.table.SceneTable;
import com.slg.table.anno.Table;
import com.slg.table.model.TableInt;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场景管理器
 *
 * @author yangxunan
 * @date 2026/2/2
 */
@Component
@Getter
public class SceneManager {

    @Table
    private TableInt<SceneTable> sceneTable;

    @Getter
    private static Scene mainScene;

    private Map<Long, Scene> scenes = new ConcurrentHashMap<>();

    private Scene[] staticScenes = new Scene[10];

    @Getter
    private static SceneManager instance;

    @PostConstruct
    public void init(){
        instance = this;
    }

    public void initMainScene(){
        createScene(1);
    }

    public void createScene(int sceneConfigId){
        SceneTable sceneTable = getSceneTable(sceneConfigId);
        if (sceneTable.getType() == SceneType.MAIN_SCENE) {
            // 大地图 赛季图等常驻地图
            mainScene = new Scene(1, sceneConfigId);
            staticScenes[1] = mainScene;
        } else{
            // 普通图
            mainScene = new Scene(SceneIdCreate.SCENE.nextId(), sceneConfigId);
        }
        scenes.put(mainScene.getSceneId(), mainScene);
    }

    public SceneTable getSceneTable(int sceneId){
        return sceneTable.get(sceneId);
    }

    public Scene getScene(long sceneId){
        if (sceneId < 10) {
            return staticScenes[Math.toIntExact(sceneId)];
        }
        return scenes.get(sceneId);
    }

}
