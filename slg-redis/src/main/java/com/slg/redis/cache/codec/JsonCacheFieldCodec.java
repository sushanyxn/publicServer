package com.slg.redis.cache.codec;

import com.slg.common.util.JsonUtil;

/**
 * 默认的 JSON 缓存字段编解码器
 * <p>基本类型（String/int/long/boolean/double/float）直接使用 String.valueOf 优化，避免 JSON 开销；
 * 复杂类型使用 {@link JsonUtil} 进行 JSON 序列化/反序列化
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class JsonCacheFieldCodec implements ICacheFieldCodec {

    public static final JsonCacheFieldCodec INSTANCE = new JsonCacheFieldCodec();

    @Override
    public String encode(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (isPrimitiveWrapper(value.getClass())) {
            return String.valueOf(value);
        }
        return JsonUtil.toJson(value);
    }

    @Override
    public Object decode(String raw, Class type) {
        if (raw == null) {
            return null;
        }
        if (type == String.class) {
            return raw;
        }
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(raw);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(raw);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(raw);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(raw);
        }
        if (type == float.class || type == Float.class) {
            return Float.parseFloat(raw);
        }
        if (type == short.class || type == Short.class) {
            return Short.parseShort(raw);
        }
        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(raw);
        }
        return JsonUtil.fromJson(raw, type);
    }

    private boolean isPrimitiveWrapper(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class || clazz == Boolean.class
                || clazz == Double.class || clazz == Float.class || clazz == Short.class
                || clazz == Byte.class;
    }
}
