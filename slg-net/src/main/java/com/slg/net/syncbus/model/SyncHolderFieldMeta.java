package com.slg.net.syncbus.model;

import com.slg.net.syncbus.codec.ISyncFieldEncoder;

import java.lang.invoke.MethodHandle;

/**
 * Holder 端单个同步字段的元数据
 * 包含字段名、类型、getter MethodHandle、自定义编码器和限流间隔
 *
 * @author yangxunan
 * @date 2026/02/12
 */
public class SyncHolderFieldMeta {

    /** 字段名（与 Cache 端约定一致） */
    private final String fieldName;

    /** Holder 端字段类型 */
    private final Class<?> fieldType;

    /** 从 Holder 读取字段值的 MethodHandle */
    private final MethodHandle getter;

    /** 自定义编码器（可为 null，null 时使用默认 JsonUtil.toJson） */
    private final ISyncFieldEncoder<?> encoder;

    /** 同步限流间隔（毫秒），从 @SyncField.syncInterval 转换，0 表示不限流 */
    private final long syncIntervalMs;

    public SyncHolderFieldMeta(String fieldName, Class<?> fieldType, MethodHandle getter,
                               ISyncFieldEncoder<?> encoder, long syncIntervalMs) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.getter = getter;
        this.encoder = encoder;
        this.syncIntervalMs = syncIntervalMs;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public MethodHandle getGetter() {
        return getter;
    }

    public ISyncFieldEncoder<?> getEncoder() {
        return encoder;
    }

    public long getSyncIntervalMs() {
        return syncIntervalMs;
    }
}
