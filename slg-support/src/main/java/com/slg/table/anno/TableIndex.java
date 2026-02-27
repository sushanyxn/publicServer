package com.slg.table.anno;

import java.lang.annotation.*;

/**
 * 配置表索引注解
 * 标注在字段上，表示该字段需要建立索引以支持快速查询
 * 
 * @author yangxunan
 * @date 2025-12-26
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableIndex {
    
    /**
     * 索引名称（必填）
     * 用于标识索引，通过 getIndexd(indexName, value) 查询
     */
    String value();
    
    /**
     * 是否唯一索引（可选）
     * true: 一对一关系
     * false: 一对多关系（默认）
     */
    boolean unique() default false;
    
}
