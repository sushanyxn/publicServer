package com.slg.sharedmodules.attribute.type;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 属性类型枚举。
 * <p>
 * 定义游戏中所有属性类型，分为基础属性（由模块投放）和计算属性（由公式推导）。
 * 百分比属性以万分比表示（10000 = 100%）。
 * </p>
 *
 * @author yangxunan
 * @date 2026-03-12
 */
@Getter
public enum AttributeType {

    // ==================== 攻击 ====================
    /** 基础攻击力 */
    BASE_ATK(1, "基础攻击力"),
    /** 攻击加成（万分比） */
    ATK_PCT(2, "攻击加成"),
    /** 额外攻击力 */
    EXTRA_ATK(3, "额外攻击力"),
    /** 最终攻击力（计算属性） */
    FINAL_ATK(4, "最终攻击力"),

    // ==================== 防御 ====================
    /** 基础防御力 */
    BASE_DEF(11, "基础防御力"),
    /** 防御加成（万分比） */
    DEF_PCT(12, "防御加成"),
    /** 额外防御力 */
    EXTRA_DEF(13, "额外防御力"),
    /** 最终防御力（计算属性） */
    FINAL_DEF(14, "最终防御力"),

    // ==================== 生命 ====================
    /** 基础生命值 */
    BASE_HP(21, "基础生命值"),
    /** 生命加成（万分比） */
    HP_PCT(22, "生命加成"),
    /** 额外生命值 */
    EXTRA_HP(23, "额外生命值"),
    /** 最终生命值（计算属性） */
    FINAL_HP(24, "最终生命值"),

    ;

    private static final Map<Integer, AttributeType> ID_MAP = new HashMap<>();

    static {
        for (AttributeType type : values()) {
            ID_MAP.put(type.id, type);
        }
    }

    private final int id;
    private final String name;

    AttributeType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 根据 id 查找属性类型
     *
     * @param id 属性类型 id
     * @return 对应的属性类型，不存在时返回 null
     */
    public static AttributeType fromId(int id) {
        return ID_MAP.get(id);
    }
}
