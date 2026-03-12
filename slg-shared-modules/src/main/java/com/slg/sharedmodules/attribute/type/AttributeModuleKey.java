package com.slg.sharedmodules.attribute.type;

/**
 * 属性模块组合键。
 * <p>
 * 由 {@link AttributeModule} + 子级 id 组成，用于区分同一模块下的不同属性来源。
 * 例如装备模块下，id 可区分头部装备、武器装备等不同部位。
 * id 为 0 时表示该模块的默认/整体属性。
 * </p>
 *
 * @param module 属性来源模块
 * @param id     模块内子级标识，0 为默认
 * @author yangxunan
 * @date 2026-03-12
 */
public record AttributeModuleKey(AttributeModule module, int id) {

    @Override
    public String toString() {
        return id == 0
                ? module.getName()
                : module.getName() + "(" + id + ")";
    }
}
