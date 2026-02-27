package com.slg.table.anno;

import java.lang.annotation.*;

/**
 * 复合索引字段注解
 * 标注在字段上，多个字段可以组成一个复合索引
 * 
 * <p>使用示例：
 * <pre>
 * &#64;TableCompositeIndexField(name = "IDX_HERO_LEVEL", order = 0)
 * private int heroId;
 * 
 * &#64;TableCompositeIndexField(name = "IDX_HERO_LEVEL", order = 1)
 * private int level;
 * </pre>
 * 
 * @author yangxunan
 * @date 2026/01/14
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(TableCompositeIndexFields.class)
public @interface TableCompositeIndexField {
    
    /**
     * 复合索引名称（必填）
     * 相同索引名的字段会组成一个复合索引
     */
    String name();
    
    /**
     * 在复合索引中的顺序（必填）
     * 从 0 开始，按顺序组合索引键
     */
    int order();
    
    /**
     * 是否唯一索引（可选）
     * 只需要在任意一个字段上设置即可（建议在 order=0 的字段上设置）
     */
    boolean unique() default false;
}






