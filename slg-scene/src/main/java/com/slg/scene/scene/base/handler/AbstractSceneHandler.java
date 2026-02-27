package com.slg.scene.scene.base.handler;

import com.slg.common.constant.SceneType;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.base.manager.SceneManager;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.base.table.SceneTable;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

/**
 * 场景handler
 *
 * @author yangxunan
 * @date 2026/2/2
 */
public abstract class AbstractSceneHandler {

    private static final Map<SceneType, AbstractSceneHandler> sceneHandlers = new HashMap<SceneType, AbstractSceneHandler>();

    @PostConstruct
    public void init(){
        AbstractSceneHandler.sceneHandlers.put(this.getSceneType(), this);
    }

    public static AbstractSceneHandler getHandler(SceneType sceneType){
        return sceneHandlers.get(sceneType);
    }

    public static Scene getScene(ScenePlayer player){
        if (player.getSceneConfigId() <= 0) {
            return null;
        }
        SceneTable sceneTable = SceneManager.getInstance().getSceneTable(player.getSceneConfigId());
        if (sceneTable == null) {
            return null;
        }
        return getHandler(sceneTable.getType()).getScene(player, player.getSceneConfigId());
    }

    /**
     * 场景类型
     *
     * @return
     */
    public abstract SceneType getSceneType();

    /**
     * 进入场景判断
     *
     * @param player  玩家
     * @param sceneId 场景id
     * @return
     */
    public abstract int verifyEnter(ScenePlayer player, int sceneId);

    /**
     * 正式进入场景
     *
     * @param player  玩家
     * @param sceneId 场景id
     * @return
     */
    public abstract int enterScene(ScenePlayer player, int sceneId);

    /**
     * 离开场景
     *
     * @param player  玩家
     * @param sceneId 场景id
     */
    public abstract void exitScene(ScenePlayer player, int sceneId);

    /**
     * 获取刚进入场景时的视野位置
     *
     * @param player
     * @param scene
     * @return
     */
    public abstract Position getEnterInitPosition(ScenePlayer player, Scene scene);

    /**
     * 根据玩家和场景配置拿到对应的场景实体
     * 如果是获取副本场景，场景进程应该有预存玩家和对应场景实体的绑定关系
     *
     * @param player
     * @param sceneConfigId
     * @return
     */
    public abstract Scene getScene(ScenePlayer player, int sceneConfigId);

}
