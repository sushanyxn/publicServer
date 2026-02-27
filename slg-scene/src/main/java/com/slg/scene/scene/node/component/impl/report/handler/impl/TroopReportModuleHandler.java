package com.slg.scene.scene.node.component.impl.report.handler.impl;

import com.slg.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.FightTroopVO;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.net.message.clientmessage.report.packet.TroopModuleVO;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.report.handler.AbstractReportModuleHandler;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.SceneNode;
import org.springframework.stereotype.Component;

/**
 * 战报兵种模块 Handler。
 * 构建双方参战兵种的数量与伤亡（初始、轻伤、重伤、死亡、剩余）。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Component
public class TroopReportModuleHandler extends AbstractReportModuleHandler {

    @Override
    public ReportModuleTypeEnum getModuleType() {
        return ReportModuleTypeEnum.TROOP;
    }

    @Override
    public ReportModuleVO buildModule(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                     ArmyDetailComponent<?> attackerArmyDetail, ArmyDetailComponent<?> defenderArmyDetail,
                                     FightContext fightContext) {
        TroopModuleVO vo = new TroopModuleVO();
        vo.setAttackerTroops(fightContext.getAttacker() != null ? fightContext.getAttacker().toFightTroopVOs() : new FightTroopVO[0]);
        vo.setDefenderTroops(fightContext.getDefender() != null ? fightContext.getDefender().toFightTroopVOs() : new FightTroopVO[0]);
        return vo;
    }
}
