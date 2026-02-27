package com.slg.scene.scene.node.owner;

import com.slg.net.message.clientmessage.scene.packet.MonsterOwnerVO;
import com.slg.net.message.clientmessage.scene.packet.OwnerVO;
import lombok.Getter;
import lombok.Setter;

/**
 * 中立NPC
 *
 * @author yangxunan
 * @date 2026/2/4
 */
@Getter
@Setter
public class NpcOwner extends NodeOwner {

    @Override
    public OwnerVO toOwnerVO() {
        return new MonsterOwnerVO();
    }
}
