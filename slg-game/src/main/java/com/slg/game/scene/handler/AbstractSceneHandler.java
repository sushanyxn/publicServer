package com.slg.game.scene.handler;

import com.slg.common.constant.SceneType;
import com.slg.game.base.player.model.Player;
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

    /**
     * 场景类型
     * @return
     */
   public abstract SceneType getSceneType();

    /**
     * 进入场景判断
     *
     * @param player        玩家
     * @param sceneServerId 场景服id
     * @param sceneId       场景id
     * @return
     */
   public abstract void verifyEnter(Player player, int sceneServerId, int sceneId);

    /**
     * 正式进入场景
     *
     * @param player        玩家
     * @param sceneServerId
     * @param sceneId
     * @return
     */
   public abstract void enterScene(Player player, int sceneServerId, int sceneId);

    /**
     * 离开场景
     *
     * @param player        玩家
     * @param sceneServerId
     * @param sceneId       场景id
     */
   public abstract void exitScene(Player player, int sceneServerId, int sceneId);

}
