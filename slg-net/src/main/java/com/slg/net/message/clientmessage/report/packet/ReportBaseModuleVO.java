package com.slg.net.message.clientmessage.report.packet;

import com.slg.net.message.clientmessage.scene.packet.OwnerVO;
import com.slg.net.message.clientmessage.scene.packet.PositionVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 战报基础模块 VO。
 * 描述进攻方、防守方、战斗胜负及战斗地点等基础信息。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportBaseModuleVO extends ReportModuleVO {

    /** 进攻方信息 */
    private OwnerVO attacker;

    /** 防守方信息 */
    private OwnerVO defender;

    /** 是否进攻方胜利 */
    private boolean attackerWin;

    /** 战斗发生地点 */
    private PositionVO position;

}
