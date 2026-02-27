package com.slg.redis.cache.entity;

import com.slg.redis.cache.CacheModule;
import com.slg.redis.cache.anno.CacheEntity;
import com.slg.redis.cache.anno.CacheField;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yangxunan
 * @date 2026/2/25
 */
@CacheEntity(module = CacheModule.PLAYER)
@Getter
@Setter
public class PlayerRedisCache {

    @CacheField
    private long playerId;

    @CacheField
    private String name;

}
