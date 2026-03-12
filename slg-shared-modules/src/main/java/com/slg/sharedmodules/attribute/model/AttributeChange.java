package com.slg.sharedmodules.attribute.model;

import com.slg.sharedmodules.attribute.type.AttributeType;

/**
 * 属性变化记录。
 * <p>
 * 记录单个属性在一次操作中从旧值到新值的变化。
 * </p>
 *
 * @param type     变化的属性类型
 * @param oldValue 变化前的值
 * @param newValue 变化后的值
 * @author slgserver
 * @date 2026-03-12
 */
public record AttributeChange(AttributeType type, long oldValue, long newValue) {

    /**
     * 变化差值（newValue - oldValue）
     *
     * @return 差值
     */
    public long delta() {
        return newValue - oldValue;
    }
}
