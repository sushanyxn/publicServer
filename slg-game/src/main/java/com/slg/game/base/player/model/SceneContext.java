package com.slg.game.base.player.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yangxunan
 * @date 2025/12/23
 */
@Getter
@Setter
public class SceneContext {

    private Player player;

    /**
     * 当前的场景
     */
    private int currentSceneId;

    /**
     * 当前的场景服
     */
    private int currentSceneServerId;

    /**
     * 准备切换到的场景
     */
    private int goingSceneId;

    /**
     * 准备切换到的场景服
     */
    private int goingSceneServerId;

    /**
     * 已经进行过绑定的场景服 (该场景服有玩家的ScenePlayer)
     */
    private Set<Integer> bindSceneServerIds = new HashSet<Integer>();

    /**
     * 场景初始化标识
     * volatile：跨线程可见性保证（Player 链写入、Login 链读取）
     */
    private volatile boolean sceneInit;

    public SceneContext(Player player) {
        this.player = player;
    }

    public boolean verifyBindSceneServer(int sceneServerId){
        return bindSceneServerIds.contains(sceneServerId);
    }

    public void bindSceneServer(int sceneServerId){
        bindSceneServerIds.add(sceneServerId);
    }

    public void updateScene(int sceneServerId, int sceneId){
        this.currentSceneId = sceneId;
        this.currentSceneServerId = sceneServerId;
    }

    public void updateGoingScene(int sceneServerId, int sceneId){
        this.goingSceneId = sceneId;
        this.goingSceneServerId = sceneServerId;
    }

    public void clearGoingCache(){
        this.goingSceneId = -1;
        this.goingSceneServerId = -1;
    }

}
