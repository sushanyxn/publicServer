package com.slg.net.syncbus.model;

import com.slg.net.syncbus.SyncModule;

import java.util.Map;

/**
 * Holder 端实体元数据
 * 包含同步模块、Holder 类信息和所有同步字段的元数据
 *
 * @author yangxunan
 * @date 2026/02/12
 */
public class SyncHolderMeta {

    /** 所属同步模块 */
    private final SyncModule syncModule;

    /** Holder 端实体类 */
    private final Class<?> holderClass;

    /** 字段名 -> 字段元数据 映射 */
    private final Map<String, SyncHolderFieldMeta> fields;

    public SyncHolderMeta(SyncModule syncModule, Class<?> holderClass, Map<String, SyncHolderFieldMeta> fields) {
        this.syncModule = syncModule;
        this.holderClass = holderClass;
        this.fields = fields;
    }

    public SyncModule getSyncModule() {
        return syncModule;
    }

    public Class<?> getHolderClass() {
        return holderClass;
    }

    public Map<String, SyncHolderFieldMeta> getFields() {
        return fields;
    }
}
