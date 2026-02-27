package com.slg.net.message.clientmessage.scene.packet;

import lombok.Data;

/**
 * 场景节点 VO（抽象基类）
 * 
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
public abstract class SceneNodeVO {

    /** 节点实体 id（场景内唯一） */
    private long id;

    /** 节点归属（玩家/联盟/怪物等） */
    private OwnerVO owner;

}
