package com.slg.game.gm.model;

import lombok.Getter;

import java.lang.reflect.Method;

/**
 * GM 方法元数据
 * 记录一个已注册 GM 方法的 bean 实例、反射 Method 对象和用户参数类型
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
public class GMMethodMeta {

    /** GM 方法所属的 bean 实例 */
    private final Object bean;

    /** 反射方法对象 */
    private final Method method;

    /** 用户参数类型（不含第一个 Player 参数） */
    private final Class<?>[] paramTypes;

    public GMMethodMeta(Object bean, Method method, Class<?>[] paramTypes) {
        this.bean = bean;
        this.method = method;
        this.paramTypes = paramTypes;
    }

}
