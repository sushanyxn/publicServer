package com.slg.net.message.clientmessage.scene.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 需要透传到scene的player信息
 *
 * @author yangxunan
 * @date 2026/2/2
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class ScenePlayerVO {

    /** 玩家 id */
    private long playerId;

    /** 玩家所在游戏服 serverId */
    private int gameServerId;

    /**
     * 玩家联盟
     */
    private long allianceId;

}
