package com.slg.redis.cache.accessor;

import com.slg.common.log.LoggerUtil;
import com.slg.redis.cache.meta.CacheEntityMeta;
import com.slg.redis.cache.meta.CacheFieldMeta;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 泛型缓存访问器
 * 基于 Redis Hash 实现复杂对象的字段级读写，支持单实体和批量操作
 * <p>每个 @CacheEntity 类对应一个 CacheAccessor 实例，通过 {@code @CacheAccessorInject} 注入
 *
 * @param <T> 缓存实体类型
 * @author yangxunan
 * @date 2026-02-25
 */
public class CacheAccessor<T> {

    private final CacheEntityMeta meta;
    private final StringRedisTemplate stringRedisTemplate;
    private final boolean clusterMode;

    /** 批量操作超时时间 */
    private static final Duration BATCH_TIMEOUT = Duration.ofSeconds(10);

    public CacheAccessor(CacheEntityMeta meta, StringRedisTemplate stringRedisTemplate,
                         RedisConnectionFactory connectionFactory) {
        this.meta = meta;
        this.stringRedisTemplate = stringRedisTemplate;
        this.clusterMode = isClusterMode(connectionFactory);
    }

    // ==================== 单实体写操作 ====================

    /**
     * 写单字段
     *
     * @param entityId  实体标识
     * @param fieldName 字段名（推荐使用 Lombok Fields 常量）
     * @param value     字段值
     */
    public void setField(Object entityId, String fieldName, Object value) {
        CacheFieldMeta fieldMeta = requireFieldMeta(fieldName);
        String key = meta.buildKey(entityId);
        String encoded = fieldMeta.encode(value);
        stringRedisTemplate.opsForHash().put(key, fieldName, encoded);
    }

    /**
     * 批量写字段（同一实体的多个字段）
     *
     * @param entityId    实体标识
     * @param fieldValues 字段名 -> 字段值 的映射
     */
    public void setFields(Object entityId, Map<String, Object> fieldValues) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return;
        }
        String key = meta.buildKey(entityId);
        Map<String, String> encoded = new LinkedHashMap<>(fieldValues.size());
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            CacheFieldMeta fieldMeta = requireFieldMeta(entry.getKey());
            String val = fieldMeta.encode(entry.getValue());
            if (val != null) {
                encoded.put(entry.getKey(), val);
            }
        }
        if (!encoded.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(key, encoded);
        }
    }

    /**
     * 写入整个对象的所有 @CacheField 字段
     *
     * @param entityId 实体标识
     * @param obj      缓存对象
     */
    public void setAll(Object entityId, T obj) {
        if (obj == null) {
            return;
        }
        String key = meta.buildKey(entityId);
        Map<String, String> encoded = encodeAllFields(obj);
        if (!encoded.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(key, encoded);
        }
    }

    // ==================== 单实体读操作 ====================

    /**
     * 读单字段（返回原始值）
     *
     * @param entityId  实体标识
     * @param fieldName 字段名
     * @param clazz     目标类型
     * @param <V>       返回值类型
     * @return 字段值，不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <V> V getField(Object entityId, String fieldName, Class<V> clazz) {
        CacheFieldMeta fieldMeta = requireFieldMeta(fieldName);
        String key = meta.buildKey(entityId);
        Object raw = stringRedisTemplate.opsForHash().get(key, fieldName);
        if (raw == null) {
            return null;
        }
        return (V) fieldMeta.decode(raw.toString());
    }

    /**
     * 读多个字段（返回原始值 Map）
     *
     * @param entityId   实体标识
     * @param fieldNames 字段名列表
     * @return fieldName -> 解码后的值
     */
    public Map<String, Object> getFields(Object entityId, String... fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) {
            return Collections.emptyMap();
        }
        for (String fn : fieldNames) {
            meta.validateFieldName(fn);
        }
        String key = meta.buildKey(entityId);
        List<Object> hashKeys = new ArrayList<>(fieldNames.length);
        Collections.addAll(hashKeys, fieldNames);

        List<Object> values = stringRedisTemplate.opsForHash().multiGet(key, hashKeys);
        Map<String, Object> result = new LinkedHashMap<>(fieldNames.length);
        for (int i = 0; i < fieldNames.length; i++) {
            Object raw = values.get(i);
            if (raw != null) {
                CacheFieldMeta fieldMeta = meta.getFieldMeta(fieldNames[i]);
                result.put(fieldNames[i], fieldMeta.decode(raw.toString()));
            }
        }
        return result;
    }

    /**
     * 读取所有字段并组装为对象
     *
     * @param entityId 实体标识
     * @return 缓存对象，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public T getAll(Object entityId) {
        String key = meta.buildKey(entityId);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return null;
        }
        return (T) assembleObject(entries);
    }

    /**
     * 读取指定字段并组装为对象（未指定的字段保持默认值）
     *
     * @param entityId   实体标识
     * @param fieldNames 需要加载的字段名
     * @return 缓存对象（只有指定字段被赋值），不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public T get(Object entityId, String... fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) {
            return getAll(entityId);
        }
        for (String fn : fieldNames) {
            meta.validateFieldName(fn);
        }
        String key = meta.buildKey(entityId);
        List<Object> hashKeys = new ArrayList<>(fieldNames.length);
        Collections.addAll(hashKeys, fieldNames);

        List<Object> values = stringRedisTemplate.opsForHash().multiGet(key, hashKeys);

        boolean anyNonNull = false;
        Map<Object, Object> partial = new LinkedHashMap<>(fieldNames.length);
        for (int i = 0; i < fieldNames.length; i++) {
            Object raw = values.get(i);
            if (raw != null) {
                partial.put(fieldNames[i], raw);
                anyNonNull = true;
            }
        }
        if (!anyNonNull) {
            return null;
        }
        return (T) assembleObject(partial);
    }

    // ==================== 批量实体读操作 ====================

    /**
     * 批量获取多个实体的所有字段
     * <p>Standalone/Sentinel 使用 Pipeline，Cluster 使用 Lettuce 原生异步命令
     *
     * @param entityIds 实体标识集合
     * @return entityId -> 缓存对象，不存在的实体不包含在结果中
     */
    public Map<Object, T> batchGetAll(Collection<?> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (clusterMode) {
            return batchGetCluster(entityIds);
        }
        return batchGetPipeline(entityIds);
    }

    /**
     * 批量获取多个实体的指定字段（未指定的字段保持默认值）
     * <p>Standalone/Sentinel 使用 Pipeline，Cluster 使用 Lettuce 原生异步命令
     *
     * @param entityIds  实体标识集合
     * @param fieldNames 需要加载的字段名
     * @return entityId -> 缓存对象，不存在的实体不包含在结果中
     */
    public Map<Object, T> batchGet(Collection<?> entityIds, String... fieldNames) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (fieldNames == null || fieldNames.length == 0) {
            return batchGetAll(entityIds);
        }
        for (String fn : fieldNames) {
            meta.validateFieldName(fn);
        }
        if (clusterMode) {
            return batchGetClusterPartial(entityIds, fieldNames);
        }
        return batchGetPipelinePartial(entityIds, fieldNames);
    }

    // ==================== 通用操作 ====================

    /**
     * 删除整个缓存对象
     *
     * @param entityId 实体标识
     * @return true 删除成功
     */
    public Boolean delete(Object entityId) {
        String key = meta.buildKey(entityId);
        return stringRedisTemplate.delete(key);
    }

    /**
     * 批量删除
     *
     * @param entityIds 实体标识集合
     * @return 删除的数量
     */
    public Long delete(Collection<?> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return 0L;
        }
        List<String> keys = new ArrayList<>(entityIds.size());
        for (Object id : entityIds) {
            keys.add(meta.buildKey(id));
        }
        return stringRedisTemplate.delete(keys);
    }

    /**
     * 设置过期时间
     *
     * @param entityId 实体标识
     * @param timeout  过期时间
     * @param unit     时间单位
     * @return true 设置成功
     */
    public Boolean expire(Object entityId, long timeout, TimeUnit unit) {
        String key = meta.buildKey(entityId);
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    /**
     * 检查缓存是否存在
     *
     * @param entityId 实体标识
     * @return true 存在
     */
    public Boolean exists(Object entityId) {
        String key = meta.buildKey(entityId);
        return stringRedisTemplate.hasKey(key);
    }

    // ==================== Pipeline 批量实现（Standalone / Sentinel） ====================

    @SuppressWarnings("unchecked")
    private Map<Object, T> batchGetPipeline(Collection<?> entityIds) {
        List<Object> idList = new ArrayList<>(entityIds);
        List<Object> results = stringRedisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (Object id : idList) {
                byte[] keyBytes = meta.buildKey(id).getBytes();
                connection.hashCommands().hGetAll(keyBytes);
            }
            return null;
        });

        Map<Object, T> resultMap = new LinkedHashMap<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            Object raw = results.get(i);
            if (raw instanceof Map<?, ?> map && !map.isEmpty()) {
                Map<Object, Object> stringMap = convertBytesMap(map);
                if (!stringMap.isEmpty()) {
                    resultMap.put(idList.get(i), (T) assembleObject(stringMap));
                }
            }
        }
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, T> batchGetPipelinePartial(Collection<?> entityIds, String... fieldNames) {
        List<Object> idList = new ArrayList<>(entityIds);
        byte[][] fieldBytes = new byte[fieldNames.length][];
        for (int i = 0; i < fieldNames.length; i++) {
            fieldBytes[i] = fieldNames[i].getBytes();
        }

        List<Object> results = stringRedisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (Object id : idList) {
                byte[] keyBytes = meta.buildKey(id).getBytes();
                connection.hashCommands().hMGet(keyBytes, fieldBytes);
            }
            return null;
        });

        Map<Object, T> resultMap = new LinkedHashMap<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            Object raw = results.get(i);
            if (raw instanceof List<?> valueList && !valueList.isEmpty()) {
                Map<Object, Object> partial = buildPartialMapFromList(fieldNames, valueList);
                if (!partial.isEmpty()) {
                    resultMap.put(idList.get(i), (T) assembleObject(partial));
                }
            }
        }
        return resultMap;
    }

    // ==================== Lettuce 异步批量实现（Cluster） ====================

    @SuppressWarnings("unchecked")
    private Map<Object, T> batchGetCluster(Collection<?> entityIds) {
        RedisAdvancedClusterAsyncCommands<String, String> async = getClusterAsyncCommands();
        if (async == null) {
            return batchGetFallback(entityIds);
        }

        List<Object> idList = new ArrayList<>(entityIds);
        List<RedisFuture<Map<String, String>>> futures = new ArrayList<>(idList.size());
        for (Object id : idList) {
            String key = meta.buildKey(id);
            futures.add(async.hgetall(key));
        }

        awaitFutures(futures);

        Map<Object, T> resultMap = new LinkedHashMap<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            try {
                Map<String, String> hash = futures.get(i).get();
                if (hash != null && !hash.isEmpty()) {
                    Map<Object, Object> objectMap = new LinkedHashMap<>(hash);
                    resultMap.put(idList.get(i), (T) assembleObject(objectMap));
                }
            } catch (Exception e) {
                LoggerUtil.error("批量获取缓存失败, entityId={}", idList.get(i), e);
            }
        }
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, T> batchGetClusterPartial(Collection<?> entityIds, String... fieldNames) {
        RedisAdvancedClusterAsyncCommands<String, String> async = getClusterAsyncCommands();
        if (async == null) {
            return batchGetPartialFallback(entityIds, fieldNames);
        }

        List<Object> idList = new ArrayList<>(entityIds);
        List<RedisFuture<List<KeyValue<String, String>>>> futures = new ArrayList<>(idList.size());
        for (Object id : idList) {
            String key = meta.buildKey(id);
            futures.add(async.hmget(key, fieldNames));
        }

        awaitFutures(futures);

        Map<Object, T> resultMap = new LinkedHashMap<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            try {
                List<KeyValue<String, String>> kvList = futures.get(i).get();
                if (kvList != null && !kvList.isEmpty()) {
                    Map<Object, Object> partial = new LinkedHashMap<>();
                    for (KeyValue<String, String> kv : kvList) {
                        if (kv.hasValue()) {
                            partial.put(kv.getKey(), kv.getValue());
                        }
                    }
                    if (!partial.isEmpty()) {
                        resultMap.put(idList.get(i), (T) assembleObject(partial));
                    }
                }
            } catch (Exception e) {
                LoggerUtil.error("批量获取缓存字段失败, entityId={}", idList.get(i), e);
            }
        }
        return resultMap;
    }

    // ==================== 逐个回退实现（Lettuce 获取失败时兜底） ====================

    private Map<Object, T> batchGetFallback(Collection<?> entityIds) {
        Map<Object, T> resultMap = new LinkedHashMap<>(entityIds.size());
        for (Object id : entityIds) {
            T obj = getAll(id);
            if (obj != null) {
                resultMap.put(id, obj);
            }
        }
        return resultMap;
    }

    private Map<Object, T> batchGetPartialFallback(Collection<?> entityIds, String... fieldNames) {
        Map<Object, T> resultMap = new LinkedHashMap<>(entityIds.size());
        for (Object id : entityIds) {
            T obj = get(id, fieldNames);
            if (obj != null) {
                resultMap.put(id, obj);
            }
        }
        return resultMap;
    }

    // ==================== 内部辅助方法 ====================

    private CacheFieldMeta requireFieldMeta(String fieldName) {
        CacheFieldMeta fieldMeta = meta.getFieldMeta(fieldName);
        if (fieldMeta == null) {
            throw new IllegalArgumentException(
                    String.format("字段 '%s' 未在 %s 中标注 @CacheField",
                            fieldName, meta.getEntityClass().getSimpleName()));
        }
        return fieldMeta;
    }

    /**
     * 将对象的所有 @CacheField 字段编码为字符串 Map
     */
    private Map<String, String> encodeAllFields(Object obj) {
        Map<String, String> encoded = new LinkedHashMap<>();
        for (CacheFieldMeta fieldMeta : meta.getFieldMetas().values()) {
            Object value = fieldMeta.getFieldValue(obj);
            String str = fieldMeta.encode(value);
            if (str != null) {
                encoded.put(fieldMeta.getFieldName(), str);
            }
        }
        return encoded;
    }

    /**
     * 将 Redis Hash 结果组装为对象
     *
     * @param entries Redis Hash 的 field -> value 映射（value 为 String）
     * @return 组装后的缓存对象
     */
    private Object assembleObject(Map<Object, Object> entries) {
        Object obj = meta.newInstance();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String fieldName = entry.getKey().toString();
            CacheFieldMeta fieldMeta = meta.getFieldMeta(fieldName);
            if (fieldMeta == null) {
                continue;
            }
            Object decoded = fieldMeta.decode(entry.getValue().toString());
            if (decoded != null) {
                fieldMeta.setFieldValue(obj, decoded);
            }
        }
        return obj;
    }

    /**
     * Pipeline 返回的 byte[] Map 转为 String Map
     */
    private Map<Object, Object> convertBytesMap(Map<?, ?> map) {
        Map<Object, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String k = entry.getKey() instanceof byte[] bytes ? new String(bytes) : entry.getKey().toString();
            String v = entry.getValue() instanceof byte[] bytes ? new String(bytes) : entry.getValue().toString();
            result.put(k, v);
        }
        return result;
    }

    /**
     * 将 HMGET 返回的值列表与字段名配对
     */
    private Map<Object, Object> buildPartialMapFromList(String[] fieldNames, List<?> valueList) {
        Map<Object, Object> partial = new LinkedHashMap<>();
        for (int j = 0; j < fieldNames.length && j < valueList.size(); j++) {
            Object val = valueList.get(j);
            if (val != null) {
                String v = val instanceof byte[] bytes ? new String(bytes) : val.toString();
                partial.put(fieldNames[j], v);
            }
        }
        return partial;
    }

    /**
     * 判断是否为 Redis Cluster 模式
     */
    private boolean isClusterMode(RedisConnectionFactory connectionFactory) {
        if (connectionFactory instanceof LettuceConnectionFactory lettuceFactory) {
            return lettuceFactory.getClusterConfiguration() != null;
        }
        return false;
    }

    /**
     * 获取 Lettuce 集群异步命令
     *
     * @return 异步命令对象，非集群模式或获取失败返回 null
     */
    @SuppressWarnings("unchecked")
    private RedisAdvancedClusterAsyncCommands<String, String> getClusterAsyncCommands() {
        try {
            RedisConnectionFactory factory = stringRedisTemplate.getConnectionFactory();
            if (factory instanceof LettuceConnectionFactory lettuceFactory) {
                Object nativeConnection = lettuceFactory.getConnection().getNativeConnection();
                if (nativeConnection instanceof io.lettuce.core.cluster.api.StatefulRedisClusterConnection<?, ?> clusterConn) {
                    return (RedisAdvancedClusterAsyncCommands<String, String>) clusterConn.async();
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("获取 Lettuce 集群异步命令失败，将回退到逐个查询", e);
        }
        return null;
    }

    /**
     * 统一等待所有 Future 完成
     */
    private void awaitFutures(List<? extends RedisFuture<?>> futures) {
        try {
            boolean success = io.lettuce.core.LettuceFutures.awaitAll(
                    BATCH_TIMEOUT, futures.toArray(new RedisFuture[0]));
            if (!success) {
                LoggerUtil.error("批量 Redis 操作超时, module={}", meta.getModule().name());
            }
        } catch (Exception e) {
            LoggerUtil.error("批量 Redis 操作异常, module={}", meta.getModule().name(), e);
        }
    }
}
