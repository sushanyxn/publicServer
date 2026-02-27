package com.slg.scene.scene.node.component.impl.assemble;

import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.node.model.impl.AssembleArmy;

/**
 * 集结组件
 * <p>面向 {@link AssembleArmy}，处理集结等待、集结出发等业务。</p>
 *
 * @author yangxunan
 * @date 2026-02-06
 */
public class AssembleComponent extends AbstractNodeComponent<AssembleArmy> {

    public AssembleComponent(AssembleArmy belongNode) {
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.Assemble;
    }

    /**
     * 集结等待：集结阶段等待成员加入等逻辑。
     */
    public void onAssembleWaiting() {
        // 逻辑后续实现
    }

    /**
     * 集结出发：集结完成后出发至目标等逻辑。
     */
    public void onAssembleDepart() {
        // 逻辑后续实现
    }
}
