package com.slg.redis.service;

import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 通用缓存服务
 * 封装常用的 String、Hash 缓存操作
 * <p>底层基于 Spring Data Redis，兼容 standalone / cluster / sentinel 三种连接模式
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ========================= String 操作 =========================

    /**
     * 设置缓存（无过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回 null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存值并转换为指定类型
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的对象，不存在或转换失败返回 null
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        try {
            return JsonUtil.getMapper().convertValue(value, clazz);
        } catch (Exception e) {
            LoggerUtil.error("Redis 缓存值类型转换失败, key={}, targetType={}", key, clazz.getSimpleName());
            return null;
        }
    }

    /**
     * 设置字符串缓存
     *
     * @param key   缓存键
     * @param value 字符串值
     */
    public void setString(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置字符串缓存（带过期时间）
     *
     * @param key     缓存键
     * @param value   字符串值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void setString(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取字符串缓存
     *
     * @param key 缓存键
     * @return 字符串值，不存在返回 null
     */
    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return true 删除成功
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     * <p>Lettuce 在集群模式下会自动按 slot 分组发送，无需手动处理
     *
     * @param keys 缓存键集合
     * @return 删除的数量
     */
    public Long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return redisTemplate.delete(keys);
    }

    /**
     * 检查缓存键是否存在
     *
     * @param key 缓存键
     * @return true 存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true 设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取剩余过期时间（秒）
     *
     * @param key 缓存键
     * @return 剩余秒数，-1 表示永不过期，-2 表示键不存在
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 自增
     *
     * @param key   缓存键
     * @param delta 增量
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // ========================= Hash 操作 =========================

    /**
     * 设置 Hash 字段值
     *
     * @param key     Hash 键
     * @param hashKey Hash 字段
     * @param value   字段值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 批量设置 Hash 字段
     *
     * @param key Hash 键
     * @param map 字段值映射
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取 Hash 字段值
     *
     * @param key     Hash 键
     * @param hashKey Hash 字段
     * @return 字段值，不存在返回 null
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取 Hash 所有字段和值
     *
     * @param key Hash 键
     * @return 所有字段值映射
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 字段
     *
     * @param key      Hash 键
     * @param hashKeys 要删除的字段
     * @return 删除的字段数量
     */
    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 检查 Hash 字段是否存在
     *
     * @param key     Hash 键
     * @param hashKey Hash 字段
     * @return true 存在
     */
    public Boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }
}
