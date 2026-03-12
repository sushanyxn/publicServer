package com.slg.scene.scene.node.component.impl.common;

import com.slg.sharedmodules.fight.wos.FightTaskSubmit;
import com.slg.sharedmodules.fight.wos.model.FightArmy;
import com.slg.sharedmodules.fight.wos.model.FightContext;
import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.component.impl.report.ReportComponent;
import com.slg.scene.scene.node.node.model.SceneNode;
import com.slg.scene.scene.node.node.model.StaticNode;

/**
 * 战斗组件
 * <p>面向 {@link StaticNode}，当交互组件遇到战斗需求时，通过本组件提交战斗任务。</p>
 * <p>接收进攻方与防守方的 {@link ArmyDetailComponent}，由本组件内部生成 {@link FightArmy}、判空后调用 slg-shared-modules
 * 提交战斗并结算；返回结果前执行战后处理（生成战报并写入 {@link FightContext#setReport(Object)}），再返回战斗上下文。</p>
 *
 * @author yangxunan
 * @date 2026-02-06
 */
public class FightComponent extends AbstractNodeComponent<StaticNode<?>> {

    public FightComponent(StaticNode<?> belongNode) {
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.Fight;
    }

    /**
     * 根据双方节点与军队详情生成战斗数据、提交战斗并结算；战后根据传入的节点生成战报并写入上下文，返回战斗上下文。
     *
     * @param attackerNode   进攻方场景节点（由提交战斗处传入）
     * @param defenderNode   防守方场景节点（由提交战斗处传入）
     * @param attackerDetail 进攻方军队详情，为 null 时视为无效，返回 null
     * @param defenderDetail 防守方军队详情，为 null 时视为无效，返回 null
     * @return 战斗上下文（含胜负与战报），任一方无效时返回 null
     */
    public FightContext submitBattle(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                     ArmyDetailComponent<?> attackerDetail, ArmyDetailComponent<?> defenderDetail) {
        if (attackerDetail == null) {
            return null;
        }
        if (defenderDetail == null) {
            return null;
        }
        FightArmy attacker = attackerDetail.toFightArmy();
        FightArmy defender = defenderDetail.toFightArmy();
        FightContext ctx = FightTaskSubmit.submit(attacker, defender);
        handleAfterBattle(ctx, attackerNode, defenderNode, attackerDetail, defenderDetail);
        return ctx;
    }

    /**
     * 战后处理：根据提交战斗处传入的双方节点与军队详情生成战报并写入战斗上下文。
     */
    private void handleAfterBattle(FightContext ctx, SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                  ArmyDetailComponent<?> attackerDetail, ArmyDetailComponent<?> defenderDetail) {
        var report = ReportComponent.buildReport(
                attackerNode,
                defenderNode,
                attackerDetail,
                defenderDetail,
                ctx
        );
        ctx.setReport(report);
    }
}
