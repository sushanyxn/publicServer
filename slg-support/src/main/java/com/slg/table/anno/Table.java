package com.slg.table.anno;

import java.lang.annotation.*;

/**
 * 配置表注解
 *
 * @author yangxunan
 * @date 2025-12-26
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    
    /**
     * 表名默认使用类名
     * 如果不是类名，在这里声明
     */
    String alias() default "";
}
