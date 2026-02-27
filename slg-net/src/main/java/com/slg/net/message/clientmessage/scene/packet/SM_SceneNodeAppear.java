package com.slg.net.message.clientmessage.scene.packet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场景节点出现消息
 * 
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@AllArgsConstructor(staticName = "valueOf")
@NoArgsConstructor
public class SM_SceneNodeAppear {

    private SceneNodeVO node;

}
