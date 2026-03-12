package com.slg.sharedmodules.attribute.listener;

import com.slg.sharedmodules.attribute.model.AttributeChange;

import java.util.List;

/**
 * 属性变化监听器。
 * <p>
 * 当容器中的属性值发生变化（包括聚合值和计算值）时，
 * 以批量方式通知所有变化的属性。
 * </p>
 *
 * @author yangxunan
 * @date 2026-03-12
 */
@FunctionalInterface
public interface AttributeChangeListener {

    /**
     * 属性变化回调
     *
     * @param changes 本次操作中所有发生变化的属性列表
     */
    void onAttributesChanged(List<AttributeChange> changes);
}
