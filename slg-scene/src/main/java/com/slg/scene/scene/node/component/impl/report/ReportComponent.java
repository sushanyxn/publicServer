package com.slg.scene.scene.node.component.impl.report;

import com.slg.sharedmodules.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.net.message.clientmessage.report.packet.ReportVO;
import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.report.handler.AbstractReportModuleHandler;
import com.slg.scene.scene.node.component.impl.report.handler.ReportModuleTypeEnum;
import com.slg.scene.scene.node.node.model.SceneNode;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 战报组件抽象基类。
 * 继承节点组件基类，挂载到参与战斗的节点上；子类实现 {@link #getRequiredModules()} 声明本战报组件需要哪些战报模块（与类绑定，不在运行时生成）。
 * 生成战报时先通过进攻方与防守方两个战报组件的声明聚合出需要构建的模块集合，再依次调用各模块 Handler 构建并汇总为 {@link ReportVO}。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
public abstract class ReportComponent<T extends SceneNode<?>> extends AbstractNodeComponent<T> {

    public ReportComponent(T belongNode) {
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.Report;
    }

    /**
     * 本战报组件需要哪些战报模块。
     * 由子类实现并固定返回与类绑定的集合，不在运行时动态生成。
     *
     * @return 需要的模块类型集合，不应为 null，可为空集合
     */
    public abstract Set<ReportModuleTypeEnum> getRequiredModules();

    /**
     * 根据进攻方与防守方战报组件的声明，聚合出需要构建的模块集合。
     * 规则：若模块 {@link ReportModuleTypeEnum#isRequireBoth()} 为 true 则双方都声明才构建，否则任一方声明即构建。
     *
     * @param attackerComponent 进攻方战报组件
     * @param defenderComponent 防守方战报组件
     * @return 需要构建的模块类型集合
     */
    public static Set<ReportModuleTypeEnum> aggregateModulesToBuild(ReportComponent<?> attackerComponent,
                                                                     ReportComponent<?> defenderComponent) {
        Set<ReportModuleTypeEnum> attackerNeeds = attackerComponent != null ? attackerComponent.getRequiredModules() : Set.of();
        Set<ReportModuleTypeEnum> defenderNeeds = defenderComponent != null ? defenderComponent.getRequiredModules() : Set.of();
        Set<ReportModuleTypeEnum> result = EnumSet.noneOf(ReportModuleTypeEnum.class);
        for (ReportModuleTypeEnum moduleType : ReportModuleTypeEnum.values()) {
            boolean attacker = attackerNeeds.contains(moduleType);
            boolean defender = defenderNeeds.contains(moduleType);
            if (moduleType.isRequireBoth()) {
                if (attacker && defender) {
                    result.add(moduleType);
                }
            } else {
                if (attacker || defender) {
                    result.add(moduleType);
                }
            }
        }
        return result;
    }

    /**
     * 传入构造战报模块所需参数，从双方节点上获取战报组件并聚合声明的模块，再依次构建所有需要的模块并返回 ReportVO。
     * 战报组件通过 {@link ComponentEnum#Report} 从 node 上获取，无该组件时视为该方不声明任何模块。
     *
     * @param attackerNode       进攻方场景节点
     * @param defenderNode       防守方场景节点
     * @param attackerArmyDetail 进攻方军队详情组件
     * @param defenderArmyDetail 防守方军队详情组件
     * @param fightContext       战斗结束后的上下文
     * @return 组装好的战报 VO，reportModules 已按 typeId 填入各模块 VO
     */
    public static ReportVO buildReport(SceneNode<?> attackerNode,
                                      SceneNode<?> defenderNode,
                                      ArmyDetailComponent<?> attackerArmyDetail,
                                      ArmyDetailComponent<?> defenderArmyDetail,
                                      FightContext fightContext) {
        ReportComponent<?> attackerComponent = attackerNode.getComponent(ComponentEnum.Report);
        ReportComponent<?> defenderComponent = defenderNode.getComponent(ComponentEnum.Report);
        Set<ReportModuleTypeEnum> toBuild = aggregateModulesToBuild(attackerComponent, defenderComponent);
        Map<Integer, ReportModuleVO> reportModules = new LinkedHashMap<>();
        for (ReportModuleTypeEnum moduleType : toBuild) {
            AbstractReportModuleHandler handler = AbstractReportModuleHandler.getHandler(moduleType);
            if (handler == null) {
                continue;
            }
            ReportModuleVO moduleVO = handler.buildModule(attackerNode, defenderNode,
                    attackerArmyDetail, defenderArmyDetail, fightContext);
            if (moduleVO != null) {
                reportModules.put(moduleType.getTypeId(), moduleVO);
            }
        }
        ReportVO reportVO = new ReportVO();
        reportVO.setReportModules(reportModules);
        return reportVO;
    }
}
