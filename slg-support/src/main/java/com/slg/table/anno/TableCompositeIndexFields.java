package com.slg.table.anno;

import java.lang.annotation.*;

/**
 * 复合索引字段容器注解（支持一个字段属于多个复合索引）
 * 
 * @author yangxunan
 * @date 2026/01/14
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableCompositeIndexFields {
    TableCompositeIndexField[] value();
}






