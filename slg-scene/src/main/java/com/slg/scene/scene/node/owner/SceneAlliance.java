package com.slg.scene.scene.node.owner;

import com.slg.net.message.clientmessage.scene.packet.AllianceOwnerVO;
import com.slg.net.message.clientmessage.scene.packet.OwnerVO;

/**
 * 联盟
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public class SceneAlliance extends NodeOwner {

    @Override
    public AllianceOwnerVO toOwnerVO() {
        AllianceOwnerVO vo = new AllianceOwnerVO();
        vo.setAllianceId(getId());
        return vo;
    }
}
