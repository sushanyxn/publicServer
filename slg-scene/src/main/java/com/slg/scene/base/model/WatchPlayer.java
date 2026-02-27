package com.slg.scene.base.model;

/**
 * 观察者player 不具有业务能力
 * 只能观察场景
 *
 * @author yangxunan
 * @date 2026/2/2
 */
public class WatchPlayer extends ScenePlayer{
    public WatchPlayer(long playerId){
        super(playerId);
    }
}
