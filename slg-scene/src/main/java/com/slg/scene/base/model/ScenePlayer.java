package com.slg.scene.base.model;

import com.slg.scene.base.entity.ScenePlayerEntity;
import com.slg.scene.scene.base.handler.AbstractSceneHandler;
import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.node.node.model.impl.PlayerArmy;
import com.slg.scene.scene.node.node.model.impl.PlayerCity;
import com.slg.scene.scene.node.owner.NodeOwner;
import com.slg.net.message.clientmessage.scene.packet.PlayerOwnerVO;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 场景玩家
 * 代表一个在场景服务器中的玩家
 *
 * @author yangxunan
 * @date 2026/1/23
 */
@Getter
@Setter
public class ScenePlayer extends NodeOwner {

    // 所在场景
    protected int sceneConfigId;

    // 场景缓存实体
    private ScenePlayerEntity scenePlayerEntity;

    // 关联玩家军队队列 包括处于别人集结中的子部队
    private Map<Long, PlayerArmy> armies = new HashMap<>();

    // 关联玩家主城
    private PlayerCity mainCity;

    /**
     * 场景业务是否已完成初始化（节点已创建）
     * volatile：跨线程可见性保证（PLAYER 链写 true，SCENE 链写 false）
     * 不参与持久化，重启后所有玩家需重新由 Game 触发初始化
     */
    private volatile boolean sceneInited;


    public ScenePlayer(long playerId){
        this.id = playerId;
    }

    public ScenePlayer(ScenePlayerEntity scenePlayerEntity){
        this.id = scenePlayerEntity.getPlayerId();
        this.scenePlayerEntity = scenePlayerEntity;
    }

    public int getGameServerId(){
        return scenePlayerEntity.getGameServerId();
    }

    public long getAllianceId(){
        return scenePlayerEntity.getAllianceId();
    }

    @Override
    public PlayerOwnerVO toOwnerVO(){
        PlayerOwnerVO vo = new PlayerOwnerVO();
        vo.setPlayerId(getId());
        vo.setAllianceId(getAllianceId());
        return vo;
    }

    public Scene getScene(){
        return AbstractSceneHandler.getScene(this);
    }
}
