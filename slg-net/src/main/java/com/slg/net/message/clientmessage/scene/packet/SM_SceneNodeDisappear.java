package com.slg.net.message.clientmessage.scene.packet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场景节点消失消息
 * 
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class SM_SceneNodeDisappear {

    /** 消失的节点实体 id（场景内唯一） */
    private long id;

}
