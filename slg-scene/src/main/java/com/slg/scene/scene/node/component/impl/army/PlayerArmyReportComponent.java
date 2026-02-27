package com.slg.scene.scene.node.component.impl.army;

import com.slg.scene.scene.node.component.impl.report.ReportComponent;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.impl.PlayerArmy;

import java.util.EnumSet;
import java.util.Set;

/**
 * 玩家军队战报组件。
 * 挂载于 {@link PlayerArmy}，声明单队玩家战斗战报需要的模块：基础、英雄、兵种、属性、录像、科技数据。
 * 与类绑定，不在运行时变更。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
public class PlayerArmyReportComponent extends ReportComponent<PlayerArmy> {

    private static final Set<ReportModuleTypeEnum> REQUIRED = EnumSet.of(
            // 战报头 基础战斗信息
            ReportModuleTypeEnum.BASE,
            // 英雄养成对比
            ReportModuleTypeEnum.HERO,
            // 兵力战损对比
            ReportModuleTypeEnum.TROOP,
            // 属性养成对比
            ReportModuleTypeEnum.ATTRIBUTE,
            // 录像回看
            ReportModuleTypeEnum.VIDEO,
            // 科技养成对比
            ReportModuleTypeEnum.TECHNOLOGY
    );

    public PlayerArmyReportComponent(PlayerArmy belongNode) {
        super(belongNode);
    }

    @Override
    public Set<ReportModuleTypeEnum> getRequiredModules() {
        return REQUIRED;
    }
}
