package com.slg.sharedmodules.attribute.type;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 属性来源模块枚举。
 * <p>
 * 标识属性由哪个系统投放，用于模块化管理属性贡献。
 * 通过 {@link #key()} 或 {@link #key(int)} 生成 {@link AttributeModuleKey}，
 * 支持同一模块下的子级区分（如装备的不同部位）。
 * </p>
 *
 * @author yangxunan
 * @date 2026-03-12
 */
@Getter
public enum AttributeModule {

    /** 英雄基础属性 */
    HERO(1, "英雄"),

    /** 装备属性 */
    EQUIP(2, "装备"),

    /** 科技属性 */
    TECHNOLOGY(3, "科技"),

    /** 增益效果（Buff）属性 */
    BUFF(4, "增益"),

    ;

    private static final Map<Integer, AttributeModule> ID_MAP = new HashMap<>();

    static {
        for (AttributeModule module : values()) {
            ID_MAP.put(module.id, module);
        }
    }

    private final int id;
    private final String name;

    AttributeModule(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 根据 id 查找属性来源模块
     *
     * @param id 模块 id
     * @return 对应的模块，不存在时返回 null
     */
    public static AttributeModule fromId(int id) {
        return ID_MAP.get(id);
    }

    /**
     * 生成默认模块键（子级 id = 0）
     *
     * @return 模块键
     */
    public AttributeModuleKey key() {
        return new AttributeModuleKey(this, 0);
    }

    /**
     * 生成指定子级 id 的模块键，用于同一模块下的细分场景
     *
     * @param subId 子级标识（如装备部位 id）
     * @return 模块键
     */
    public AttributeModuleKey key(int subId) {
        return new AttributeModuleKey(this, subId);
    }
}
