package com.slg.scene.scene.node.component.impl.player;

import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.camp.CampUtil;
import com.slg.scene.scene.node.component.*;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.army.IdleComponent;
import com.slg.scene.scene.node.component.impl.common.DestroyComponent;
import com.slg.scene.scene.node.component.impl.common.GarrisonComponent;
import com.slg.sharedmodules.fight.wos.model.FightContext;
import com.slg.scene.scene.node.component.impl.common.FightComponent;
import com.slg.scene.scene.node.component.impl.common.InteractiveComponent;
import com.slg.scene.scene.node.model.ArmyActionType;
import com.slg.scene.scene.node.node.model.RouteNode;
import com.slg.scene.scene.node.node.model.impl.PlayerCity;
import com.slg.scene.scene.node.owner.NodeOwner;

/**
 * 玩家主城交互组件
 * <p>先根据军队拥有者与主城拥有者判断阵营关系，再结合行军目的（{@link ArmyActionType}）决定交互。</p>
 * <ul>
 *   <li><b>驻扎/参与集结</b>：仅友方（不含自己），己方或敌方/中立返回 false</li>
 *   <li><b>敌对</b>：仅 canAttack 时执行攻击</li>
 *   <li><b>缺省及未 case 的目的</b>：完全按阵营关系——SELF 回城、canGarrison 驻守、canAttack 攻击</li>
 *   <li>己方军队与己方主城交互时，触发军队回城 handler（{@link IdleComponent#onReturnToCity()}）</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public class PlayerCityInteractiveComponent extends InteractiveComponent<PlayerCity> {

    public PlayerCityInteractiveComponent(PlayerCity belongNode) {
        super(belongNode);
    }

    @Override
    protected boolean doOnInteractedBy(RouteNode<?> arrivingNode, ArmyActionType purpose) {
        NodeOwner armyOwner = arrivingNode.getOwner();
        NodeOwner cityOwner = belongNode.getOwner();
        CampType relation = CampUtil.getCampRelation(armyOwner, cityOwner);

        switch (purpose) {
            case GARRISON -> {
                return handleGarrison(arrivingNode, relation);
            }
            case JOIN_ASSEMBLE -> {
                return handleJoinAssemble(arrivingNode, relation);
            }
            case ATTACK -> {
                return handleAttack(arrivingNode, relation);
            }
            default -> {
                // DEFAULT 及未显式 case 的枚举，统一按阵营关系处理
                return handleDefault(arrivingNode, relation);
            }
        }
    }

    /**
     * 驻扎：仅友方（不含自己）可驻防
     */
    private boolean handleGarrison(RouteNode<?> arrivingNode, CampType relation) {
        if (relation == CampType.SELF) {
            return false;
        }
        if (!relation.isCanGarrison()) {
            return false;
        }
        GarrisonComponent garrison = belongNode.getComponent(ComponentEnum.Garrison);
        if (garrison != null) {
            garrison.assignGarrisonArmy(arrivingNode);
        }
        return true;
    }

    /**
     * 参与集结
     */
    private boolean handleJoinAssemble(RouteNode<?> arrivingNode, CampType relation) {



        return true;
    }

    /**
     * 敌对：仅 canAttack 时执行。获取本节点战斗组件，传入来访军队与驻防军队的 ArmyDetailComponent，提交战斗；战后生成战报并写入上下文，可根据胜负与战报做后续逻辑。
     */
    private boolean handleAttack(RouteNode<?> arrivingNode, CampType relation) {
        if (!relation.isCanAttack()) {
            return false;
        }
        FightComponent fight = belongNode.getComponent(ComponentEnum.Fight);
        if (fight == null) {
            return false;
        }
        ArmyDetailComponent<?> attackerDetail = arrivingNode.getComponent(ComponentEnum.ArmyDetail);
        ArmyDetailComponent<?> defenderDetail = getDefenderArmyDetail();
        FightContext ctx = fight.submitBattle(arrivingNode, belongNode, attackerDetail, defenderDetail);
        if (ctx == null) {
            return false;
        }
        // 可根据 ctx.isAttackerWin()、ctx.getReport() 做后续逻辑（如占领、掠夺、下发热报等）
        return true;
    }

    /**
     * 需要获取玩家的内城兵力和城门驻防英雄，以及驻防组件中的驻防兵力
     */
    private ArmyDetailComponent<?> getDefenderArmyDetail() {

        return null;
    }

    /**
     * 缺省逻辑：完全按阵营关系——SELF 回城，canGarrison 驻守，canAttack 攻击
     */
    private boolean handleDefault(RouteNode<?> arrivingNode, CampType relation) {
        if (relation == CampType.SELF) {
            DestroyComponent<?> destroy = arrivingNode.getComponent(ComponentEnum.Destroy);
            if (destroy != null) {
                destroy.onDestroy();
            }
            return true;
        }
        if (relation.isCanGarrison()) {
            GarrisonComponent garrison = belongNode.getComponent(ComponentEnum.Garrison);
            if (garrison != null) {
                garrison.assignGarrisonArmy(arrivingNode);
            }
            return true;
        }
        if (relation.isCanAttack()) {
            return handleAttack(arrivingNode, relation);
        }
        return false;
    }
}
