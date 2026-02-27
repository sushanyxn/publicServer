package com.slg.scene.scene.node.component.impl.common;

import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.node.model.SceneNode;

/**
 * 销毁组件（抽象基类）
 * <p>表示节点具有「被销毁/解散」时的处理能力，由子类实现具体逻辑（如军队回城时解散、从场景移除等）。</p>
 * <p>军队回城等场景应调用本组件处理，而非 Idle 组件。</p>
 *
 * @param <T> 挂载的节点类型
 * @author yangxunan
 * @date 2026/2/5
 */
public abstract class DestroyComponent<T extends SceneNode<?>> extends AbstractNodeComponent<T> {

    public DestroyComponent(T belongNode) {
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.Destroy;
    }

    /**
     * 节点被销毁/解散时调用（如军队回城时由交互方触发）。
     * 子类实现具体逻辑（从场景移除、归还兵力等）。
     */
    public abstract void onDestroy();
}
