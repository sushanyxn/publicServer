package com.slg.scene.scene.node.component.impl.army;

import com.slg.fight.wos.model.FightArmy;
import com.slg.net.message.clientmessage.army.packet.ArmyVO;
import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.node.model.RouteNode;

/**
 * 军队详情组件抽象基类
 * 用于描述行军节点的军队信息并可生成 {@link ArmyVO}，由 {@link PlayerArmyDetailComponent}
 * 与 {@link MultiArmyDetailComponent} 共同继承，使用同一组件类型 {@link ComponentEnum#ArmyDetail}。
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public abstract class ArmyDetailComponent<T extends RouteNode<?>> extends AbstractNodeComponent<T> {

    public ArmyDetailComponent(T belongNode) {
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.ArmyDetail;
    }

    /**
     * 根据当前详情数据生成军队 VO
     *
     * @return 军队 VO，具体子类型由子类决定（如 PlayerArmyVO、AssembleArmyVO）
     */
    public abstract ArmyVO toArmyVO();

    /**
     * 根据当前详情数据生成战斗用军队数据（FightArmy）。
     * 单军队取本队英雄与兵种；多军队取队长的英雄、所有人的士兵合并。
     * 无数据时返回英雄与兵种均为空数组的 FightArmy，不返回 null。
     *
     * @return 战斗一方军队数据，永不为 null
     */
    public abstract FightArmy toFightArmy();
}
