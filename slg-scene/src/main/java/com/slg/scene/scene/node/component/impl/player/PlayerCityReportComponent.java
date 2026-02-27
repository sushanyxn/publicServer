package com.slg.scene.scene.node.component.impl.player;

import com.slg.scene.scene.node.component.impl.report.ReportComponent;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.impl.PlayerCity;

import java.util.EnumSet;
import java.util.Set;

/**
 * 玩家主城战报组件。
 * 挂载于 {@link PlayerCity}（防守方），声明主城参与战斗时需要的战报模块：基础、英雄、兵种、属性、录像、科技数据。
 * 与类绑定，不在运行时变更。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
public class PlayerCityReportComponent extends ReportComponent<PlayerCity> {

    private static final Set<ReportModuleTypeEnum> REQUIRED = EnumSet.of(
            ReportModuleTypeEnum.BASE,
            ReportModuleTypeEnum.HERO,
            ReportModuleTypeEnum.TROOP,
            ReportModuleTypeEnum.ATTRIBUTE,
            ReportModuleTypeEnum.VIDEO,
            ReportModuleTypeEnum.TECHNOLOGY
    );

    public PlayerCityReportComponent(PlayerCity belongNode) {
        super(belongNode);
    }

    @Override
    public Set<ReportModuleTypeEnum> getRequiredModules() {
        return REQUIRED;
    }
}
