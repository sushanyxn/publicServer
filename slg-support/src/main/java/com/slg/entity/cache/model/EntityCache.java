package com.slg.entity.cache.model;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.slg.common.executor.core.GlobalScheduler;
import com.slg.common.log.LoggerUtil;
import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.cache.writer.WriteBehindBuffer;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.entity.db.persist.AsyncPersistenceService;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 实体缓存类
 * 为特定实体类型提供缓存优先的数据访问
 * 每个实体类型应该有自己的 EntityCache 实例
 *
 * @author yangxunan
 * @date 2025-12-18
 * @param <T> 实体类型，继承自 BaseEntity
 */
@Getter
public class EntityCache<T extends BaseEntity<?>> {

    /**
     * 此缓存管理的实体类
     */
    private final Class<T> entityClass;

    /**
     * 异步持久化服务
     * 统一的数据库操作入口
     */
    private final AsyncPersistenceService asyncPersistenceService;

    /**
     * Caffeine 缓存实例
     */
    private final Cache<Object, T> cache;

    /**
     * 字段反射缓存
     * Key: 字段名, Value: Field 对象
     * 在构造时预加载所有字段，避免运行时反射开销
     */
    private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();

    /**
     * Write-Behind 缓冲区（可选）
     */
    private final WriteBehindBuffer<T> writeBehindBuffer;

    /**
     * 缓存未命中时是否跳过数据库查询（仅查缓存，不查库）
     */
    private final boolean skipDbOnMiss;

    /**
     * 构造函数
     * 自动读取实体类上的 @CacheConfig 注解来配置缓存参数
     *
     * @param entityClass 实体类
     * @param asyncPersistenceService 异步持久化服务
     */
    public EntityCache(Class<T> entityClass, AsyncPersistenceService asyncPersistenceService, GlobalScheduler globalScheduler) {
        this.entityClass = entityClass;
        this.asyncPersistenceService = asyncPersistenceService;
        
        // 读取实体类上的 @CacheConfig 注解
        CacheConfig cacheConfig = entityClass.getAnnotation(CacheConfig.class);
        long maxSize = 5000;
        long expireMinutes = 30;
        boolean autoSaveOnExpire = true;
        boolean writeBehind = true;
        long writeBehindIntervalSeconds = 60;
        int batchSaveSize = 50;
        boolean skipDbOnMiss = false;
        
        if (cacheConfig != null) {
            maxSize = cacheConfig.maxSize();
            expireMinutes = cacheConfig.expireMinutes();
            autoSaveOnExpire = cacheConfig.autoSaveOnExpire();
            writeBehind = cacheConfig.writeDelay();
            writeBehindIntervalSeconds = cacheConfig.writeDelaySec();
            batchSaveSize = cacheConfig.batchSaveSize();
            skipDbOnMiss = cacheConfig.skipDbOnMiss();
        }
        
        this.skipDbOnMiss = skipDbOnMiss;
        
        // 构建缓存
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        
        // 配置最大数量：-1 表示不限制大小
        if (maxSize > 0) {
            builder.maximumSize(maxSize);
        } else if (maxSize == -1) {
            // 不限制大小
        } else {
            throw new IllegalArgumentException(
                    String.format("Invalid maxSize=%d for %s, must be positive or -1", 
                            maxSize, entityClass.getSimpleName()));
        }
        
        // 配置过期时间
        if (expireMinutes > 0) {
            builder.expireAfterAccess(expireMinutes, TimeUnit.MINUTES);
        } else if (expireMinutes == -1) {
            // 不过期
        } else {
            throw new IllegalArgumentException(
                    String.format("Invalid expireMinutes=%d for %s, must be positive or -1", 
                            expireMinutes, entityClass.getSimpleName()));
        }
        
        // 启用统计
        builder.recordStats();
        
        // 预加载所有字段到缓存中（包括父类字段）
        initFieldCache();
        
        // 根据配置决定是否启用 Write-Behind 模式
        if (writeBehind) {
            // 验证间隔配置
            if (writeBehindIntervalSeconds <= 0) {
                throw new IllegalArgumentException(
                    String.format("Invalid writeBehindIntervalSeconds=%d for %s, must be positive", 
                        writeBehindIntervalSeconds, entityClass.getSimpleName()));
            }
            
            // 创建 Write-Behind 缓冲区（秒转换为毫秒）
            long batchIntervalMs = writeBehindIntervalSeconds * 1000;
            this.writeBehindBuffer = new WriteBehindBuffer<>(
                entityClass, asyncPersistenceService, batchIntervalMs, batchSaveSize, globalScheduler
            );
        } else {
            this.writeBehindBuffer = null;
            LoggerUtil.debug("{} 未启用 Write-Behind 模式", entityClass.getSimpleName());
        }
        
        // 配置移除监听器
        if (autoSaveOnExpire) {
            builder.removalListener((Object key, T value, RemovalCause cause) -> {
                if (cause == RemovalCause.EXPIRED && value != null) {
                    if (writeBehindBuffer != null) {
                        // Write-Behind 模式：过期实体写入缓冲区，由 flush 统一走 saveBatch 落盘
                        // 避免直接调用 save(entityId 分链) 与 saveBatch(className 分链) 并行写入同一实体
                        writeBehindBuffer.writeEntity(value);
                    } else {
                        asyncPersistenceService.save(value);
                    }
                }
            });
        }
        
        this.cache = builder.build();
    }

    /**
     * 初始化字段缓存
     * 遍历实体类及其父类的所有字段，预先缓存 Field 对象
     */
    private void initFieldCache() {
        Class<?> currentClass = entityClass;
        int fieldCount = 0;
        
        // 遍历类层次结构，直到 BaseEntity 或 Object
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            
            for (Field field : fields) {
                // 跳过 static 和 transient 字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                
                // 设置可访问并缓存
                field.setAccessible(true);
                fieldCache.put(field.getName(), field);
                fieldCount++;
            }
            
            // 继续处理父类
            currentClass = currentClass.getSuperclass();
        }
        
    }

    /**
     * 根据 ID 查找实体（缓存优先策略）
     * 1. 首先尝试从缓存获取
     * 2. 若未配置 skipDbOnMiss，则缓存未命中时从数据库加载并放入缓存
     * 3. 若配置了 skipDbOnMiss，则缓存未命中时直接返回 null，不查库
     *
     * @param id 实体 ID
     * @return 实体对象，未找到返回 null
     */
    public T findById(Object id) {
        if (id == null) {
            return null;
        }
        
        // 先尝试从缓存获取
        T cachedEntity = cache.getIfPresent(id);
        
        if (cachedEntity != null) {
            return cachedEntity;
        }
        
        if (skipDbOnMiss) {
            return null;
        }
        
        // 缓存未命中，异步从数据库加载（等待结果）
        T entity = asyncPersistenceService.findById(id, entityClass).join();
        
        // 如果找到则放入缓存
        if (entity != null) {
            cache.put(id, entity);
        }
        
        return entity;
    }

    /**
     * 保存实体
     *
     * @param entity 要保存的实体
     * @return 实体（立即返回）
     */
    public T save(T entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }

        // 更新缓存
        cache.put(entity.getId(), entity);

        if (writeBehindBuffer != null) {
            // Write-Behind 模式：加入缓冲区
            writeBehindBuffer.writeEntity(entity);
        } else {
            // 非 Write-Behind 模式：直接异步保存到数据库
            asyncPersistenceService.save(entity);
        }
        
        return entity;
    }

    /**
     * 保存单个字段到数据库
     *
     * @param entity 已修改的实体对象
     * @param fieldName 字段名（建议使用 Lombok @FieldNameConstants 生成的常量）
     */
    public void saveField(T entity, String fieldName) {
        if (entity == null || entity.getId() == null || fieldName == null) {
            return;
        }

        // 从缓存中获取字段值
        Object value = getFieldValue(entity, fieldName);
        
        if (writeBehindBuffer != null) {
            // Write-Behind 模式：加入字段级缓冲区
            writeBehindBuffer.writeField(entity.getId(), fieldName, value);
        } else {
            // 非 Write-Behind 模式：直接异步更新字段
            asyncPersistenceService.updateField(entity.getId(), fieldName, value, entityClass);
        }
    }

    /**
     * 使用缓存的反射对象获取字段值
     * 
     * @param entity 实体对象
     * @param fieldName 字段名
     * @return 字段值，读取失败返回 null
     */
    private Object getFieldValue(T entity, String fieldName) {
        Field field = fieldCache.get(fieldName);
        
        if (field == null) {
            LoggerUtil.error("字段不存在或未缓存: {}, field={}", 
                entityClass.getSimpleName(), fieldName);
            return null;
        }
        
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            LoggerUtil.error("无法访问字段: {}, field={}", 
                entityClass.getSimpleName(), fieldName, e);
            return null;
        }
    }

    /**
     * 插入新实体并放入缓存
     * <p>Write-Behind 模式下写入缓冲区（由 flush 统一走 saveBatch/upsert 落盘），
     * 避免 insert(entityId 链) 与 saveBatch(className 链) 对同一实体并行写入。
     * 对于新实体，upsert 等价于 insert，语义不变。
     *
     * @param entity 要插入的实体
     * @return 实体（立即返回）
     */
    public T insert(T entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        
        // 先放入缓存
        cache.put(entity.getId(), entity);
        
        if (writeBehindBuffer != null) {
            writeBehindBuffer.writeEntity(entity);
        } else {
            asyncPersistenceService.insert(entity);
        }
        
        return entity;
    }

    /**
     * 根据 ID 删除实体（软删除）
     * 取消待写入、从缓存移除、异步标记数据库中 deleted=true
     * <p>Write-Behind 模式下走 writeBehind 分链（与 saveBatch 同链），
     * 避免 saveBatch 的 upsert 把 deleted 写回 false 导致"复活"。
     *
     * @param id 实体 ID
     * @return 1（假设删除成功）
     */
    public long deleteById(Object id) {
        if (id == null) {
            return 0;
        }
        
        // 取消 WriteBehindBuffer 中该实体的待写入，防止后续 flush "复活"
        if (writeBehindBuffer != null) {
            writeBehindBuffer.cancelPendingWrites(id);
        }
        
        // 从缓存移除
        cache.invalidate(id);
        
        if (writeBehindBuffer != null) {
            asyncPersistenceService.deleteByIdOnWriteBehindChain(id, entityClass);
        } else {
            asyncPersistenceService.deleteById(id, entityClass);
        }
        
        return 1;
    }

    public void loadAll(){
        CompletableFuture<List<T>> future = asyncPersistenceService.findAll(entityClass);
        List<T> result = future.join();
        for (T t : result) {
            cache.put(t.getId(), t);
        }
    }

    public void loadAll(String field, Object value){
        CompletableFuture<List<T>> future = asyncPersistenceService.findByField(field, value, entityClass);
        List<T> result = future.join();
        for (T t : result) {
            cache.put(t.getId(), t);
        }
    }

    /**
     * 获取缓存中的所有实体
     * 返回当前缓存中所有的实体对象（不查询数据库）
     *
     * @return 缓存中所有的实体集合
     */
    public Collection<T> getAllCache(){
        return cache.asMap().values();
    }

    /**
     * 从缓存中清除指定 ID 的实体
     * 不影响数据库中的数据
     *
     * @param id 实体 ID
     */
    public void evict(Object id) {
        if (id != null) {
            cache.invalidate(id);
        }
    }

    /**
     * 清空此实体类型的所有缓存
     * 不影响数据库中的数据
     */
    public void clear() {
        cache.invalidateAll();
    }

    /**
     * 强制刷新（立即写入数据库）
     * 仅在 Write-Behind 模式下有效
     */
    public void flush() {
        if (writeBehindBuffer != null) {
            writeBehindBuffer.flushBuffer();
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计字符串
     */
    public String getStats() {
        return cache.stats().toString();
    }

    /**
     * 获取缓存大小
     *
     * @return 估算的缓存大小
     */
    public long size() {
        return cache.estimatedSize();
    }
}
