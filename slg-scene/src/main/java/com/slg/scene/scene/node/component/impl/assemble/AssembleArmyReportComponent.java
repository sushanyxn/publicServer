package com.slg.scene.scene.node.component.impl.assemble;

import com.slg.scene.scene.node.component.impl.report.ReportComponent;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.impl.AssembleArmy;

import java.util.EnumSet;
import java.util.Set;

/**
 * 集结军队战报组件。
 * 挂载于 {@link AssembleArmy}，声明集结战需要的战报模块：基础、英雄、兵种、属性、成员、录像、科技数据。
 * 与类绑定，不在运行时变更。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
public class AssembleArmyReportComponent extends ReportComponent<AssembleArmy> {

    private static final Set<ReportModuleTypeEnum> REQUIRED = EnumSet.of(
            ReportModuleTypeEnum.BASE,
            ReportModuleTypeEnum.HERO,
            ReportModuleTypeEnum.TROOP,
            ReportModuleTypeEnum.ATTRIBUTE,
            ReportModuleTypeEnum.MEMBER,
            ReportModuleTypeEnum.VIDEO,
            ReportModuleTypeEnum.TECHNOLOGY
    );

    public AssembleArmyReportComponent(AssembleArmy belongNode) {
        super(belongNode);
    }

    @Override
    public Set<ReportModuleTypeEnum> getRequiredModules() {
        return REQUIRED;
    }
}
