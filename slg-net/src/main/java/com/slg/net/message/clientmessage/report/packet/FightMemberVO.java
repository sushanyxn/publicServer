package com.slg.net.message.clientmessage.report.packet;

import com.slg.net.message.clientmessage.scene.packet.OwnerVO;

/**
 * 战斗成员 VO。
 * 描述集结等场景下单个成员的拥有者信息及其兵种战斗数据。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
public class FightMemberVO {

    /** 成员拥有者信息 */
    private OwnerVO owner;

    /** 该成员的兵种战斗数据（可为多类兵种聚合或代表） */
    private FightTroopVO fightTroop;

}
