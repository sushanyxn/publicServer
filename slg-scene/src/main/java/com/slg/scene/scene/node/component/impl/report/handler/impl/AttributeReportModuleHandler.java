package com.slg.scene.scene.node.component.impl.report.handler.impl;

import com.slg.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.AttributeModuleVO;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.report.handler.AbstractReportModuleHandler;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.SceneNode;
import org.springframework.stereotype.Component;

/**
 * 战报属性模块 Handler。
 * 构建双方在战斗中的属性数据（展示属性、加成属性等），后续可从 FightContext、军队详情组件或战报扩展中补充。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Component
public class AttributeReportModuleHandler extends AbstractReportModuleHandler {

    @Override
    public ReportModuleTypeEnum getModuleType() {
        return ReportModuleTypeEnum.ATTRIBUTE;
    }

    @Override
    public ReportModuleVO buildModule(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                     ArmyDetailComponent<?> attackerArmyDetail, ArmyDetailComponent<?> defenderArmyDetail,
                                     FightContext fightContext) {
        AttributeModuleVO vo = new AttributeModuleVO();
        // attackerAttribute、defenderAttribute 待战斗属性数据就绪后从 fightContext、军队详情组件或 FightRecord 填充
        return vo;
    }
}
