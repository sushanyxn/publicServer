package com.slg.common.event.anno;

import java.lang.annotation.*;

/**
 * 事件监听器注解
 * 用于标记方法作为事件监听器，该方法将在对应事件发布时被同步调用
 * 
 * @author yangxunan
 * @date 2026/1/28
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {
    
    /**
     * 执行顺序，数值越小优先级越高
     */
    int order() default 100000;
}
