package com.slg.entity.mysql.converter;

import com.slg.common.util.JsonUtil;
import com.slg.entity.mysql.anno.Serialized;
import jakarta.persistence.AttributeConverter;

import java.lang.reflect.ParameterizedType;

/**
 * 泛型 JSON 列转换器
 * 为 MySQL 实体中的复杂字段提供 JSON 序列化/反序列化支持
 *
 * <p>使用时只需创建一个空的子类指定泛型类型即可：
 * <pre>
 * {@code
 * @Converter
 * public class SomeInfoConverter extends JsonColumnConverter<SomeInfo> {}
 * }
 * </pre>
 *
 * <p>然后在实体字段上使用：
 * <pre>
 * {@code
 * @Convert(converter = SomeInfoConverter.class)
 * @Column(columnDefinition = "json")
 * private SomeInfo someInfo;
 * }
 * </pre>
 *
 * @author yangxunan
 * @date 2026/02/24
 * @param <T> 要转换的目标类型
 * @deprecated 推荐使用 {@link Serialized} 注解替代，无需为每种类型编写独立的 Converter 子类
 */
@Deprecated
public abstract class JsonColumnConverter<T> implements AttributeConverter<T, String> {

    private final Class<T> targetType;

    @SuppressWarnings("unchecked")
    protected JsonColumnConverter() {
        this.targetType = (Class<T>) ((ParameterizedType)
            getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    public String convertToDatabaseColumn(T attribute) {
        return attribute == null ? null : JsonUtil.toJson(attribute);
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        return dbData == null ? null : JsonUtil.fromJson(dbData, targetType);
    }
}
