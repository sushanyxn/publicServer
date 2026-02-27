package com.slg.net.message.clientmessage.scene.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 联盟所有者 VO
 * 
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AllianceOwnerVO extends OwnerVO{

    private long allianceId;

    private String allianceShortName;

    private String allianceName;

    private String allianceFlag;
}
