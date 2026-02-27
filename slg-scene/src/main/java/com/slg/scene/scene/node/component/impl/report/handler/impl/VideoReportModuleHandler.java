package com.slg.scene.scene.node.component.impl.report.handler.impl;

import com.slg.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.net.message.clientmessage.report.packet.VideoModuleVO;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.report.handler.AbstractReportModuleHandler;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.SceneNode;
import org.springframework.stereotype.Component;

/**
 * 战报录像模块 Handler。
 * 构建本场战报关联的录像 id，用于客户端拉取或播放战斗回放。
 * 录像 id 待 FightRecord 或上下文扩展后从 fightContext 中获取。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Component
public class VideoReportModuleHandler extends AbstractReportModuleHandler {

    @Override
    public ReportModuleTypeEnum getModuleType() {
        return ReportModuleTypeEnum.VIDEO;
    }

    @Override
    public ReportModuleVO buildModule(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                     ArmyDetailComponent<?> attackerArmyDetail, ArmyDetailComponent<?> defenderArmyDetail,
                                     FightContext fightContext) {
        VideoModuleVO vo = new VideoModuleVO();
        vo.setVideoId(0L);
        // 待 FightRecord 或 FightContext 提供录像 id 后赋值
        return vo;
    }
}
