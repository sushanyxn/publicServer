package com.slg.scene.scene.node.component.impl.report.handler.impl;

import com.slg.sharedmodules.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.FightMemberVO;
import com.slg.net.message.clientmessage.report.packet.MemberModuleVO;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.report.handler.AbstractReportModuleHandler;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.SceneNode;
import org.springframework.stereotype.Component;

/**
 * 战报成员模块 Handler。
 * 用于集结战等场景，构建双方各成员（拥有者 + 兵种）信息；具体成员数据待从 MultiFightArmy 或场景层补充。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Component
public class MemberReportModuleHandler extends AbstractReportModuleHandler {

    @Override
    public ReportModuleTypeEnum getModuleType() {
        return ReportModuleTypeEnum.MEMBER;
    }

    @Override
    public ReportModuleVO buildModule(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                     ArmyDetailComponent<?> attackerArmyDetail, ArmyDetailComponent<?> defenderArmyDetail,
                                     FightContext fightContext) {
        MemberModuleVO vo = new MemberModuleVO();
        vo.setAttackerMembers(new FightMemberVO[0]);
        vo.setDefenderMembers(new FightMemberVO[0]);
        return vo;
    }
}
