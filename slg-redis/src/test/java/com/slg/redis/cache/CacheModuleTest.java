package com.slg.redis.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheModule 单元测试
 */
class CacheModuleTest {

    @Test
    void buildKey_returnsCachePrefixAndEntityId() {
        assertEquals("cache:player:10001", CacheModule.PLAYER.buildKey(10001));
        assertEquals("cache:player:abc", CacheModule.PLAYER.buildKey("abc"));
    }

    @Test
    void fromId_validId_returnsModule() {
        assertEquals(CacheModule.PLAYER, CacheModule.fromId(1));
    }

    @Test
    void fromId_unknownId_returnsNull() {
        assertNull(CacheModule.fromId(999));
    }

    @Test
    void getId_returnsModuleId() {
        assertEquals(1, CacheModule.PLAYER.getId());
    }

    @Test
    void getKeyPrefix_returnsPrefix() {
        assertEquals("player", CacheModule.PLAYER.getKeyPrefix());
    }
}
