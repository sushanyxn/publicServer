package com.slg.table.anno;

import java.lang.annotation.*;

/**
 * 配置表主键注解
 * 标注在字段上，表示该字段是配置表的主键（唯一标识）
 * 
 * @author yangxunan
 * @date 2025-12-26
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableId {
    
}
