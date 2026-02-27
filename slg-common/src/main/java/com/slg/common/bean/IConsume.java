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
public interface IConsume<T> {

    boolean verify(T t);

    void verifyThrow(T t);

    void consume(T t);

}
