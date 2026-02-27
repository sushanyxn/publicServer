package com.slg.game.scene.handler.impl;

import com.slg.common.constant.SceneType;
import com.slg.game.scene.handler.EmptySceneHandler;
import org.springframework.stereotype.Component;

/**
 * 玩家内城场景
 *
 * @author yangxunan
 * @date 2026/2/2
 */
@Component
public class PlayerCitySceneHandler extends EmptySceneHandler {
    @Override
    public SceneType getSceneType(){
        return SceneType.PLAYER_CITY;
    }
}
