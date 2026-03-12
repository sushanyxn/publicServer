package com.slg.common.bean;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author yangxunan
 * @date 2026/1/14
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
)
public interface IReward<T> {

    /**
     * 发放奖励
     * @param t 发放对象
     * @return
     */
    RewardResult reward(T t);

    /**
     * 发放奖励（比率）
     * @param t 发放对象
     * @param rate 1=1倍 2=2倍 支持小数
     * @return
     */
    RewardResult reward(T t, float rate);
}
