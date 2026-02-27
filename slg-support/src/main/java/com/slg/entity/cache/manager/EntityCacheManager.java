package com.slg.entity.cache.manager;

import com.slg.common.executor.GlobalScheduler;
import com.slg.entity.cache.model.EntityCache;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.entity.db.persist.AsyncPersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体缓存管理器
 * 管理不同实体类型的 EntityCache 实例
 * 提供集中式的实体缓存访问
 * 
 * @author yangxunan
 * @date 2025-12-18
 */
@Component
public class EntityCacheManager {

    @Autowired
    private AsyncPersistenceService asyncPersistenceService;
    @Autowired
    private GlobalScheduler globalScheduler;

    /**
     * 不同实体类型的缓存实例
     * 键：实体类
     * 值：EntityCache 实例
     */
    private final Map<Class<?>, EntityCache<?>> caches = new ConcurrentHashMap<>();

    /**
     * 获取或创建指定实体类型的 EntityCache
     * 每个实体类型使用单例模式，线程安全
     * ID 类型从 BaseEntity 自动推导
     *
     * @param entityClass 实体类
     * @param <T> 实体类型，继承自 BaseEntity
     * @return 该实体类型的 EntityCache 实例
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity<?>> EntityCache<T> getCache(Class<T> entityClass) {
        
        return (EntityCache<T>) caches.computeIfAbsent(entityClass, key -> 
            new EntityCache<>(entityClass, asyncPersistenceService, globalScheduler)
        );
    }

    /**
     * 检查指定实体类型的缓存是否存在
     *
     * @param entityClass 实体类
     * @return 如果缓存存在返回 true
     */
    public boolean hasCache(Class<?> entityClass) {
        return caches.containsKey(entityClass);
    }

    /**
     * 移除指定实体类型的缓存
     * 适用于动态缓存管理
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     */
    public <T extends BaseEntity<?>> void removeCache(Class<T> entityClass) {
        EntityCache<?> removed = caches.remove(entityClass);
        if (removed != null) {
            removed.clear();
        }
    }

    /**
     * 清空指定实体类型的所有缓存数据
     * 缓存实例保留，仅清除数据
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     */
    public <T extends BaseEntity<?>> void clearCache(Class<T> entityClass) {
        EntityCache<?> cache = caches.get(entityClass);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 清空所有实体类型的缓存数据
     * 慎用 - 通常用于维护操作
     */
    public void clearAllCaches() {
        caches.values().forEach(EntityCache::clear);
    }

    /**
     * 移除所有缓存实例
     * 完全销毁所有缓存
     */
    public void removeAllCaches() {
        caches.values().forEach(EntityCache::clear);
        caches.clear();
    }

    /**
     * 获取指定实体缓存的统计信息
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 缓存统计字符串，如果缓存不存在返回 null
     */
    public <T extends BaseEntity<?>> String getCacheStats(Class<T> entityClass) {
        EntityCache<?> cache = caches.get(entityClass);
        if (cache != null) {
            return String.format("%s: %s", 
                    entityClass.getSimpleName(), cache.getStats());
        }
        return null;
    }

    /**
     * 获取所有实体缓存的统计信息
     *
     * @return 实体类名到统计字符串的映射
     */
    public Map<String, String> getAllCacheStats() {
        Map<String, String> stats = new ConcurrentHashMap<>();
        caches.forEach((entityClass, cache) -> {
            stats.put(entityClass.getSimpleName(), cache.getStats());
        });
        return stats;
    }

    /**
     * 获取所有实体类型的总缓存大小
     *
     * @return 总的估算缓存大小
     */
    public long getTotalCacheSize() {
        return caches.values().stream()
                .mapToLong(EntityCache::size)
                .sum();
    }

    /**
     * 获取管理的实体类型数量
     *
     * @return 实体缓存数量
     */
    public int getCacheCount() {
        return caches.size();
    }

    /**
     * 获取所有管理的实体类
     *
     * @return 实体类到缓存的映射
     */
    public Map<Class<?>, EntityCache<?>> getAllCaches() {
        return new ConcurrentHashMap<>(caches);
    }
}
