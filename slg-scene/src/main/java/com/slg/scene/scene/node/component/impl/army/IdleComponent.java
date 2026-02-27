package com.slg.scene.scene.node.component.impl.army;

import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.component.impl.common.InteractiveComponent;
import com.slg.scene.scene.node.component.impl.common.DestroyComponent;
import com.slg.scene.scene.node.component.impl.common.SelectTargetComponent;
import com.slg.scene.scene.node.node.model.RouteNode;

/**
 * 发呆组件（无目标到达组件）抽象基类
 * <p>当行军到达时目标无交互能力（空地、或目标未挂载 {@link InteractiveComponent}）时，由 {@link SelectTargetComponent#arrived()} 调用本组件处理军队后续行为。</p>
 * <p>仅挂载于 {@link RouteNode}，具体行为由子类实现。</p>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public abstract class IdleComponent<T extends RouteNode<?>> extends AbstractNodeComponent<T> {

    protected IdleComponent(T belongNode) {
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.Idle;
    }

    /**
     * 行军无目标到达时调用（目标为空地或目标无交互组件）。
     *
     * @implSpec 子类实现具体逻辑（如驻留、回城等）
     */
    public abstract void onArrivedWithNoTarget();

    /**
     * 交互失败时调用（目标有交互组件但返回 false，如不满足阵营/目的条件）。
     * 由 {@link InteractiveComponent} 基类在交互结果为 false 时统一调用。
     *
     * @implSpec 子类实现具体逻辑（如提示、驻留等）
     */
    public abstract void onInteractionFailed();

    /**
     * 军队回城时调用（如己方军队与己方主城交互时按缺省目的触发回城）。
     * 主城交互已改为调用军队的 {@link DestroyComponent} 处理回城，本方法保留供其他场景使用。
     *
     * @implSpec 子类实现回城流程
     */
    public abstract void onReturnToCity();
}
