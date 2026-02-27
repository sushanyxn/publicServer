package com.slg.scene.scene.node.component;

import lombok.Getter;
import lombok.Setter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件容器
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
@Setter
@SuppressWarnings({"unchecked", "rawtypes"})
public class ComponentContainer {

    private Map<ComponentEnum, AbstractNodeComponent> components = new EnumMap<>(ComponentEnum.class);

    public <T extends AbstractNodeComponent> T getComponent(ComponentEnum componentEnum) {
        return (T) components.get(componentEnum);
    }

    public void registerComponent(AbstractNodeComponent component) {
        components.put(component.getComponentEnum(), component);
    }

}
