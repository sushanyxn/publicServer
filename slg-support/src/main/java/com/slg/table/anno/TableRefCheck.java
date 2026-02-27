package com.slg.table.anno;

import java.lang.annotation.*;

/**
 * 表关联检查，标注在字段上，表明这个字段是其他表的主键
 * 如果该表不存在这个主键，则说明配置有问题
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableRefCheck {

    Class<?> value();

}
