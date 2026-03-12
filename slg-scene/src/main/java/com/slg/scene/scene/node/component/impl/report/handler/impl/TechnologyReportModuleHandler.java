package com.slg.scene.scene.node.component.impl.report.handler.impl;

import com.slg.sharedmodules.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.net.message.clientmessage.report.packet.TechnologyModuleVO;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.report.handler.AbstractReportModuleHandler;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.SceneNode;
import org.springframework.stereotype.Component;

/**
 * 战报科技模块 Handler（占位/扩展）。
 * 用于战报中科技相关数据的承载，当前为占位实现，不构建模块。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Component
public class TechnologyReportModuleHandler extends AbstractReportModuleHandler {

    @Override
    public ReportModuleTypeEnum getModuleType() {
        return ReportModuleTypeEnum.TECHNOLOGY;
    }

    @Override
    public ReportModuleVO buildModule(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                     ArmyDetailComponent<?> attackerArmyDetail, ArmyDetailComponent<?> defenderArmyDetail,
                                     FightContext fightContext) {
        TechnologyModuleVO technologyModuleVO = new TechnologyModuleVO();
        return technologyModuleVO;
    }
}
