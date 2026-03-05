package com.slg.redis.cache.meta;

import com.slg.redis.cache.CacheModule;
import com.slg.redis.cache.codec.JsonCacheFieldCodec;
import com.slg.redis.cache.entity.PlayerRedisCache;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheEntityMeta / CacheFieldMeta 单元测试（手动构造 meta，不依赖扫描）
 */
class CacheEntityMetaTest {

    public static CacheEntityMeta createPlayerMeta() throws Exception {
        Field playerIdField = PlayerRedisCache.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        Field nameField = PlayerRedisCache.class.getDeclaredField("name");
        nameField.setAccessible(true);

        CacheFieldMeta playerIdMeta = new CacheFieldMeta(
                "playerId", long.class, playerIdField, JsonCacheFieldCodec.INSTANCE);
        CacheFieldMeta nameMeta = new CacheFieldMeta(
                "name", String.class, nameField, JsonCacheFieldCodec.INSTANCE);

        return new CacheEntityMeta(CacheModule.PLAYER, PlayerRedisCache.class,
                Map.of("playerId", playerIdMeta, "name", nameMeta));
    }

    @Test
    void buildKey_returnsModuleKeyFormat() throws Exception {
        CacheEntityMeta meta = createPlayerMeta();
        assertEquals("cache:player:10001", meta.buildKey(10001));
        assertEquals("cache:player:abc", meta.buildKey("abc"));
    }

    @Test
    void getModule_returnsPlayer() throws Exception {
        CacheEntityMeta meta = createPlayerMeta();
        assertEquals(CacheModule.PLAYER, meta.getModule());
    }

    @Test
    void getFieldNames_containsPlayerIdAndName() throws Exception {
        CacheEntityMeta meta = createPlayerMeta();
        assertTrue(meta.getFieldNames().contains("playerId"));
        assertTrue(meta.getFieldNames().contains("name"));
        assertEquals(2, meta.getFieldNames().size());
    }

    @Test
    void getFieldMeta_returnsMetaForField() throws Exception {
        CacheEntityMeta meta = createPlayerMeta();
        assertNotNull(meta.getFieldMeta("playerId"));
        assertEquals(long.class, meta.getFieldMeta("playerId").getFieldType());
        assertNotNull(meta.getFieldMeta("name"));
        assertNull(meta.getFieldMeta("unknown"));
    }

    @Test
    void validateFieldName_valid_doesNotThrow() throws Exception {
        CacheEntityMeta meta = createPlayerMeta();
        meta.validateFieldName("playerId");
        meta.validateFieldName("name");
    }

    @Test
    void validateFieldName_invalid_throws() throws Exception {
        CacheEntityMeta meta = createPlayerMeta();
        assertThrows(IllegalArgumentException.class, () -> meta.validateFieldName("unknown"));
    }

    @Test
    void newInstance_returnsNewPlayerRedisCache() throws Exception {
        CacheEntityMeta meta = createPlayerMeta();
        Object obj = meta.newInstance();
        assertInstanceOf(PlayerRedisCache.class, obj);
    }

    @Test
    void cacheFieldMeta_encodeDecode_roundTrip() throws Exception {
        CacheEntityMeta meta = createPlayerMeta();
        CacheFieldMeta playerIdMeta = meta.getFieldMeta("playerId");
        String encoded = playerIdMeta.encode(10002L);
        assertNotNull(encoded);
        assertEquals(10002L, playerIdMeta.decode(encoded));
    }
}
