package com.slg.scene.scene.node.component.impl.common;

import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.component.impl.army.IdleComponent;
import com.slg.scene.scene.node.model.ArmyActionType;
import com.slg.scene.scene.node.node.model.RouteNode;
import com.slg.scene.scene.node.node.model.StaticNode;

/**
 * 可被交互组件（目标方）
 * 表示该静态 node 可被行军到达的 node 交互。当行军通过 {@link SelectTargetComponent} 到达本 node 时，
 * 会在其到达方法中获取本组件并调用 {@link #onInteractedBy(RouteNode, ArmyActionType)}。
 * <p>仅挂载于 {@link StaticNode}。</p>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public abstract class InteractiveComponent<T extends StaticNode<?>> extends AbstractNodeComponent<T> {

    public InteractiveComponent(T belongNode) {
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum(){
        return ComponentEnum.Interactive;
    }

    /**
     * 当有行军 node 到达本节点时由 {@link SelectTargetComponent#arrived()} 中调用。
     * 子类实现 {@link #doOnInteractedBy} 返回交互是否成功；为 false 时本方法统一调用行军方的 {@link IdleComponent#onInteractionFailed()}。
     *
     * @param arrivingNode 到达本节点的行军 node
     * @param purpose     行军目的（选择目标时传入并保存，到达时一并传入）
     */
    public final void onInteractedBy(RouteNode<?> arrivingNode, ArmyActionType purpose) {
        if (arrivingNode == null || purpose == null) {
            return;
        }
        boolean success = doOnInteractedBy(arrivingNode, purpose);
        if (!success) {
            IdleComponent idle = arrivingNode.getComponent(ComponentEnum.Idle);
            if (idle != null) {
                idle.onInteractionFailed();
            }
        }
    }

    /**
     * 子类实现的交互逻辑。
     *
     * @param arrivingNode 到达本节点的行军 node
     * @param purpose     行军目的
     * @return true 表示交互成功，false 表示交互失败（基类将调用行军方的 {@link IdleComponent#onInteractionFailed()}）
     */
    protected abstract boolean doOnInteractedBy(RouteNode<?> arrivingNode, ArmyActionType purpose);
}
