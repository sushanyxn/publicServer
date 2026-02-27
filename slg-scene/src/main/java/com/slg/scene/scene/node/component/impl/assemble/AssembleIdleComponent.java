package com.slg.scene.scene.node.component.impl.assemble;

import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.component.impl.common.DestroyComponent;
import com.slg.scene.scene.node.component.impl.army.IdleComponent;
import com.slg.scene.scene.node.node.model.impl.AssembleArmy;

/**
 * 集结军队发呆组件
 * <p>挂载于 {@link AssembleArmy}。目标无效时（无目标到达、交互失败）调用本节点的解散组件销毁集结。</p>
 *
 * @author yangxunan
 * @date 2026-02-06
 */
public class AssembleIdleComponent extends IdleComponent<AssembleArmy> {

    public AssembleIdleComponent(AssembleArmy belongNode) {
        super(belongNode);
    }

    @Override
    public void onArrivedWithNoTarget() {
        dismissAssemble();
    }

    @Override
    public void onInteractionFailed() {
        dismissAssemble();
    }

    @Override
    public void onReturnToCity() {
        dismissAssemble();
    }

    /**
     * 调用本节点解散组件销毁集结
     */
    private void dismissAssemble() {
        DestroyComponent<?> destroy = getBelongNode().getComponent(ComponentEnum.Destroy);
        if (destroy != null) {
            destroy.onDestroy();
        }
    }
}
