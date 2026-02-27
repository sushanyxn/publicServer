package com.slg.scene.scene.node.component;

import com.slg.scene.scene.node.node.model.SceneNode;
import lombok.Getter;

/**
 * node组件 定义node具有的能力
 * 使用泛型的目的是 规范组件的使用范围
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
public abstract class AbstractNodeComponent<T extends SceneNode<?>> {

    protected T belongNode;

    public AbstractNodeComponent(T belongNode){
        this.belongNode = belongNode;
    }

    public abstract ComponentEnum getComponentEnum();

}
