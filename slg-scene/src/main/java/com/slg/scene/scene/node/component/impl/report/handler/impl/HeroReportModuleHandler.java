package com.slg.scene.scene.node.component.impl.report.handler.impl;

import com.slg.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.FightHeroVO;
import com.slg.net.message.clientmessage.report.packet.HeroModuleVO;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.report.handler.AbstractReportModuleHandler;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.SceneNode;
import org.springframework.stereotype.Component;

/**
 * 战报英雄模块 Handler。
 * 构建双方参战英雄的简要信息（英雄 id、等级）。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Component
public class HeroReportModuleHandler extends AbstractReportModuleHandler {

    @Override
    public ReportModuleTypeEnum getModuleType() {
        return ReportModuleTypeEnum.HERO;
    }

    @Override
    public ReportModuleVO buildModule(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                     ArmyDetailComponent<?> attackerArmyDetail, ArmyDetailComponent<?> defenderArmyDetail,
                                     FightContext fightContext) {
        HeroModuleVO vo = new HeroModuleVO();
        vo.setAttackerHeroes(fightContext.getAttacker() != null ? fightContext.getAttacker().toFightHeroVOs() : new FightHeroVO[0]);
        vo.setDefenderHeroes(fightContext.getDefender() != null ? fightContext.getDefender().toFightHeroVOs() : new FightHeroVO[0]);
        return vo;
    }
}
