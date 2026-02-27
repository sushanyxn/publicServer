package com.slg.net.message.clientmessage.scene.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 玩家所有者 VO
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerOwnerVO extends OwnerVO {

    /** 玩家 id */
    private long playerId;

    /** 联盟 id（0 表示无联盟） */
    private long allianceId;

    /** 头像配置 id */
    private int headerId;

    /** 主城/据点位置 */
    private PositionVO cityPosition;

}
