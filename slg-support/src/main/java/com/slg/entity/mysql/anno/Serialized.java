package com.slg.entity.mysql.anno;

import com.slg.entity.mysql.converter.SerializedUserType;
import org.hibernate.annotations.Type;

import java.lang.annotation.*;

/**
 * 统一序列化注解
 * 标注在实体字段上，自动完成 JSON 序列化/反序列化，无需为每种类型编写独立的 Converter
 *
 * <p>本注解通过 Hibernate 6 的 {@link Type} 元注解机制，
 * 将 {@link SerializedUserType} 关联到被标注的字段，
 * 运行时自动推断字段类型并按指定格式进行读写。
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * // JSON 字符串存储（默认）
 * @Serialized
 * @Column(columnDefinition = "json")
 * private SomeInfo someInfo;
 *
 * // byte[] 存储
 * @Serialized(format = SerializeFormat.BYTES)
 * @Column(columnDefinition = "blob")
 * private AnotherInfo anotherInfo;
 *
 * // 泛型容器（DataList / DataMap）
 * @Serialized(elementType = XxxInfo.class)
 * @Column(columnDefinition = "json")
 * private DataList<XxxInfo> xxxList = new DataList<>();
 * }
 * </pre>
 *
 * @author yangxunan
 * @date 2026/02/24
 * @see SerializedUserType
 * @see SerializeFormat
 */
@Type(SerializedUserType.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Serialized {

    /**
     * 序列化格式，默认 JSON 字符串
     */
    SerializeFormat format() default SerializeFormat.JSON;

    /**
     * 泛型容器的元素类型，用于 {@code DataList<E>} / {@code DataMap<K,V>} 等场景。
     * 非泛型字段保持默认 {@code void.class}，对现有用法完全向后兼容。
     */
    Class<?> elementType() default void.class;
}
