package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 战报兵种模块 VO。
 * 描述进攻方与防守方参战兵种的数量与伤亡（初始、轻伤、重伤、死亡、剩余等）。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TroopModuleVO extends ReportModuleVO {

    /** 进攻方兵种列表 */
    private FightTroopVO[] attackerTroops;

    /** 防守方兵种列表 */
    private FightTroopVO[] defenderTroops;

}
