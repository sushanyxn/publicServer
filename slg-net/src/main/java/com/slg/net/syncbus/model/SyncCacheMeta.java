package com.slg.net.syncbus.model;

import com.slg.net.syncbus.ISyncCacheResolver;
import com.slg.net.syncbus.SyncModule;

import java.util.Map;

/**
 * Cache 端实体元数据
 * 包含同步模块、Cache 类信息、所有同步字段的元数据和缓存查找器
 *
 * @author yangxunan
 * @date 2026/02/12
 */
public class SyncCacheMeta {

    /** 所属同步模块 */
    private final SyncModule syncModule;

    /** Cache 端实体类 */
    private final Class<?> cacheClass;

    /** 字段名 -> 字段元数据 映射 */
    private final Map<String, SyncCacheFieldMeta> fields;

    /** 缓存查找器（可在启动后阶段设置） */
    private ISyncCacheResolver<?> resolver;

    public SyncCacheMeta(SyncModule syncModule, Class<?> cacheClass, Map<String, SyncCacheFieldMeta> fields) {
        this.syncModule = syncModule;
        this.cacheClass = cacheClass;
        this.fields = fields;
    }

    public SyncModule getSyncModule() {
        return syncModule;
    }

    public Class<?> getCacheClass() {
        return cacheClass;
    }

    public Map<String, SyncCacheFieldMeta> getFields() {
        return fields;
    }

    public ISyncCacheResolver<?> getResolver() {
        return resolver;
    }

    public void setResolver(ISyncCacheResolver<?> resolver) {
        this.resolver = resolver;
    }
}
