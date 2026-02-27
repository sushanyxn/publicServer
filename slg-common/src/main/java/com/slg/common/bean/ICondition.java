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
public interface ICondition<T> {

    /**
     * 校验条件
     * @param t
     * @return true 校验通过 | false 校验不通过
     */
    boolean verify(T t);

}
