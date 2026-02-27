package com.slg.redis.cache.meta;

import com.slg.redis.cache.codec.ICacheFieldCodec;

import java.lang.reflect.Field;

/**
 * 缓存字段元数据
 * 持有单个 @CacheField 字段的反射信息和编解码器
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class CacheFieldMeta {

    /** 字段名（即 Redis Hash 中的 field key） */
    private final String fieldName;

    /** 字段类型 */
    private final Class<?> fieldType;

    /** 反射 Field 对象（已 setAccessible） */
    private final Field field;

    /** 编解码器实例 */
    private final ICacheFieldCodec<?> codec;

    public CacheFieldMeta(String fieldName, Class<?> fieldType, Field field, ICacheFieldCodec<?> codec) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.field = field;
        this.codec = codec;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public Field getField() {
        return field;
    }

    public ICacheFieldCodec<?> getCodec() {
        return codec;
    }

    /**
     * 将字段值编码为字符串
     *
     * @param value 字段值
     * @return 编码后的字符串
     */
    @SuppressWarnings("unchecked")
    public String encode(Object value) {
        if (value == null) {
            return null;
        }
        return ((ICacheFieldCodec<Object>) codec).encode(value);
    }

    /**
     * 将字符串解码为字段值
     *
     * @param raw Redis 中存储的原始字符串
     * @return 解码后的字段值
     */
    @SuppressWarnings("unchecked")
    public Object decode(String raw) {
        if (raw == null) {
            return null;
        }
        return ((ICacheFieldCodec<Object>) codec).decode(raw, (Class<Object>) fieldType);
    }

    /**
     * 从对象中读取该字段的值
     *
     * @param obj 缓存对象实例
     * @return 字段值
     */
    public Object getFieldValue(Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("读取缓存字段失败: " + fieldName, e);
        }
    }

    /**
     * 向对象中设置该字段的值
     *
     * @param obj   缓存对象实例
     * @param value 字段值
     */
    public void setFieldValue(Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("设置缓存字段失败: " + fieldName, e);
        }
    }
}
