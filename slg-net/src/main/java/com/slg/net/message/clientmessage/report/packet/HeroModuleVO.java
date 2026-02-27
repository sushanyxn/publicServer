package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 战报英雄模块 VO。
 * 描述进攻方与防守方参战英雄的简要信息（英雄 id、等级等）。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HeroModuleVO extends ReportModuleVO {

    /** 进攻方英雄列表 */
    private FightHeroVO[] attackerHeroes;

    /** 防守方英雄列表 */
    private FightHeroVO[] defenderHeroes;

}
