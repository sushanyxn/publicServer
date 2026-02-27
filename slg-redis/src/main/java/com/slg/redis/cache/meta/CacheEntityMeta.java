package com.slg.redis.cache.meta;

import com.slg.redis.cache.CacheModule;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 缓存实体元数据
 * 持有一个 @CacheEntity 类的模块信息和所有字段元数据
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class CacheEntityMeta {

    /** 所属缓存模块 */
    private final CacheModule module;

    /** 实体类 */
    private final Class<?> entityClass;

    /** 字段元数据映射：fieldName -> CacheFieldMeta */
    private final Map<String, CacheFieldMeta> fieldMetas;

    public CacheEntityMeta(CacheModule module, Class<?> entityClass, Map<String, CacheFieldMeta> fieldMetas) {
        this.module = module;
        this.entityClass = entityClass;
        this.fieldMetas = Collections.unmodifiableMap(fieldMetas);
    }

    public CacheModule getModule() {
        return module;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public Map<String, CacheFieldMeta> getFieldMetas() {
        return fieldMetas;
    }

    /**
     * 根据字段名获取字段元数据
     *
     * @param fieldName 字段名
     * @return 字段元数据，不存在返回 null
     */
    public CacheFieldMeta getFieldMeta(String fieldName) {
        return fieldMetas.get(fieldName);
    }

    /**
     * 获取所有字段名
     *
     * @return 字段名集合
     */
    public Collection<String> getFieldNames() {
        return fieldMetas.keySet();
    }

    /**
     * 校验字段名是否合法（已注册为 @CacheField）
     *
     * @param fieldName 字段名
     * @throws IllegalArgumentException 字段未注册时抛出
     */
    public void validateFieldName(String fieldName) {
        if (!fieldMetas.containsKey(fieldName)) {
            throw new IllegalArgumentException(
                    String.format("字段 '%s' 未在 %s 中标注 @CacheField", fieldName, entityClass.getSimpleName()));
        }
    }

    /**
     * 构建 Redis Key
     *
     * @param entityId 实体标识
     * @return 完整的 Redis Key
     */
    public String buildKey(Object entityId) {
        return module.buildKey(entityId);
    }

    /**
     * 创建实体类的空实例
     *
     * @return 新实例
     */
    public Object newInstance() {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("创建缓存对象实例失败: " + entityClass.getName(), e);
        }
    }
}
