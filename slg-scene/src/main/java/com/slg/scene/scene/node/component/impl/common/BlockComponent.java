package com.slg.scene.scene.node.component.impl.common;

import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.node.model.StaticNode;

/**
 * 阻挡组件
 * <p>静态 node 出生在 scene 时占用矩形为动态阻挡，移除时释放。出生前通过 {@link Scene#getBlockContainer()} 判断是否可落点。</p>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public class BlockComponent<T extends StaticNode<?>> extends AbstractNodeComponent<T> {

    public BlockComponent(T belongNode){
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum(){
        return ComponentEnum.Block;
    }

    /**
     * 出生前检查：所属节点占用的矩形内是否无阻挡（可落点）。
     *
     * @param scene 场景
     * @return true 表示可出生，false 表示有阻挡不能出生
     */
    public boolean blockCheck(Scene scene){
        return scene.getBlockContainer().canSpawn(belongNode);
    }

    /**
     * 出生后：将所属节点占用的矩形标记为动态阻挡。
     */
    public void blockAdd(Scene scene){
        scene.getBlockContainer().addBlock(belongNode);
    }

    /**
     * 从场景移除时：清除所属节点占用矩形的动态阻挡。
     */
    public void blockRemove(Scene scene){
        scene.getBlockContainer().removeBlock(belongNode);
    }
}
