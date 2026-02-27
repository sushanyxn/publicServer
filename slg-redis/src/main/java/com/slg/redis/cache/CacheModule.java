package com.slg.redis.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存模块枚举
 * 每个枚举值对应一类缓存实体，通过显式 id 和 keyPrefix 标识
 * <p>Redis Key 格式：{@code cache:{keyPrefix}:{entityId}}
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public enum CacheModule {

    /** 玩家缓存 */
    PLAYER(1, "player"),
    ;

    private final int id;
    private final String keyPrefix;

    /** Key 前缀常量 */
    private static final String KEY_ROOT = "cache:";

    CacheModule(int id, String keyPrefix) {
        this.id = id;
        this.keyPrefix = keyPrefix;
    }

    public int getId() {
        return id;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     * 构建完整的 Redis Key
     *
     * @param entityId 实体标识
     * @return cache:{keyPrefix}:{entityId}
     */
    public String buildKey(Object entityId) {
        return KEY_ROOT + keyPrefix + ":" + entityId;
    }

    /** id -> 枚举的快速查找表 */
    private static final Map<Integer, CacheModule> ID_MAP = new HashMap<>();

    static {
        for (CacheModule m : values()) {
            if (ID_MAP.put(m.id, m) != null) {
                throw new IllegalStateException("CacheModule id 重复: " + m.id);
            }
        }
    }

    /**
     * 根据 id 查找对应的 CacheModule
     *
     * @param id 模块 ID
     * @return 对应的枚举值，不存在时返回 null
     */
    public static CacheModule fromId(int id) {
        return ID_MAP.get(id);
    }
}
