package com.slg.scene.scene.node.component.impl.assemble;

import com.slg.scene.scene.node.component.impl.common.DestroyComponent;
import com.slg.scene.scene.node.node.model.impl.AssembleArmy;

/**
 * 集结军队解散组件
 * <p>挂载于 {@link AssembleArmy}。集结被销毁/解散时由发呆组件等调用，执行解散逻辑（如从场景移除、归还成员等）。</p>
 *
 * @author yangxunan
 * @date 2026-02-06
 */
public class AssembleDismissComponent extends DestroyComponent<AssembleArmy> {

    public AssembleDismissComponent(AssembleArmy belongNode) {
        super(belongNode);
    }

    @Override
    public void onDestroy() {
        // 具体逻辑后续实现（从场景 despawn、归还成员军队等）
    }
}
