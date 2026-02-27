package com.slg.net.syncbus.model;

import com.slg.net.syncbus.codec.ISyncFieldDecoder;

import java.lang.invoke.MethodHandle;

/**
 * Cache 端单个同步字段的元数据
 * 包含字段名、类型、setter MethodHandle 和自定义解码器
 *
 * @author yangxunan
 * @date 2026/02/12
 */
public class SyncCacheFieldMeta {

    /** 字段名 */
    private final String fieldName;

    /** Cache 端字段类型 */
    private final Class<?> fieldType;

    /** 向 Cache 写入字段值的 MethodHandle */
    private final MethodHandle setter;

    /** 自定义解码器（可为 null，null 时使用默认 JsonUtil.fromJson） */
    private final ISyncFieldDecoder<?> decoder;

    public SyncCacheFieldMeta(String fieldName, Class<?> fieldType, MethodHandle setter,
                              ISyncFieldDecoder<?> decoder) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.setter = setter;
        this.decoder = decoder;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public MethodHandle getSetter() {
        return setter;
    }

    public ISyncFieldDecoder<?> getDecoder() {
        return decoder;
    }
}
