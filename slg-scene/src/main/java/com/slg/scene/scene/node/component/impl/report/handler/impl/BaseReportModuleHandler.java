package com.slg.scene.scene.node.component.impl.report.handler.impl;

import com.slg.sharedmodules.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.ReportBaseModuleVO;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.report.handler.AbstractReportModuleHandler;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.SceneNode;
import org.springframework.stereotype.Component;

/**
 * 战报基础模块 Handler。
 * 构建进攻/防守方、胜负、战斗地点等基础信息。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Component
public class BaseReportModuleHandler extends AbstractReportModuleHandler {

    @Override
    public ReportModuleTypeEnum getModuleType() {
        return ReportModuleTypeEnum.BASE;
    }

    @Override
    public ReportModuleVO buildModule(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                     ArmyDetailComponent<?> attackerArmyDetail, ArmyDetailComponent<?> defenderArmyDetail,
                                     FightContext fightContext) {
        ReportBaseModuleVO vo = new ReportBaseModuleVO();
        vo.setAttackerWin(fightContext.isAttackerWin());
        // attacker、defender、position 可由上层从场景节点或军队详情组件、战斗地点补充
        return vo;
    }
}
