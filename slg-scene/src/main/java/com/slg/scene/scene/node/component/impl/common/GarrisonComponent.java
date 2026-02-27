package com.slg.scene.scene.node.component.impl.common;

import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.node.model.RouteNode;
import com.slg.scene.scene.node.node.model.StaticNode;
import lombok.Getter;

/**
 * 驻防组件
 * 面向 {@link StaticNode}，用于描述该静态节点上的驻守军队；驻守军队使用 {@link RouteNode} 存储，由组件自身维护，外部通过业务方法派驻/撤防。
 * <p>仅挂载于 {@link StaticNode}。</p>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
@Getter
public class GarrisonComponent extends AbstractNodeComponent<StaticNode<?>> {

    /** 驻守军队（由组件维护，不对外暴露修改） */
    private RouteNode<?> garrisonArmy;

    public GarrisonComponent(StaticNode<?> belongNode) {
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.Garrison;
    }

    /**
     * 派驻军队驻防
     *
     * @param army 行军节点（驻守军队），可为 null 表示清空
     */
    public void assignGarrisonArmy(RouteNode<?> army) {
        this.garrisonArmy = army;
    }

    /**
     * 撤防，清除当前驻守军队
     */
    public void clearGarrisonArmy() {
        this.garrisonArmy = null;
    }

    /**
     * 是否有驻守军队
     */
    public boolean hasGarrisonArmy() {
        return garrisonArmy != null;
    }

}
