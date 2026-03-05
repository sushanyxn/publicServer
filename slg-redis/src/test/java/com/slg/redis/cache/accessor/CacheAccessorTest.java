package com.slg.redis.cache.accessor;

import com.slg.redis.cache.CacheModule;
import com.slg.redis.cache.codec.JsonCacheFieldCodec;
import com.slg.redis.cache.entity.PlayerRedisCache;
import com.slg.redis.cache.meta.CacheEntityMeta;
import com.slg.redis.cache.meta.CacheFieldMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CacheAccessor 单元测试（Mock StringRedisTemplate）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheAccessorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("rawtypes")
    @Mock
    private HashOperations hashOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    private CacheEntityMeta meta;
    private CacheAccessor<com.slg.redis.cache.entity.PlayerRedisCache> accessor;

    @BeforeEach
    void setUp() throws Exception {
        meta = createPlayerMeta();
        when(stringRedisTemplate.opsForHash()).thenReturn((HashOperations) hashOperations);
        accessor = new CacheAccessor<>(meta, stringRedisTemplate, connectionFactory);
    }

    private static CacheEntityMeta createPlayerMeta() throws Exception {
        java.lang.reflect.Field playerIdField = PlayerRedisCache.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        java.lang.reflect.Field nameField = PlayerRedisCache.class.getDeclaredField("name");
        nameField.setAccessible(true);
        CacheFieldMeta playerIdMeta = new CacheFieldMeta("playerId", long.class, playerIdField, JsonCacheFieldCodec.INSTANCE);
        CacheFieldMeta nameMeta = new CacheFieldMeta("name", String.class, nameField, JsonCacheFieldCodec.INSTANCE);
        return new CacheEntityMeta(CacheModule.PLAYER, PlayerRedisCache.class, Map.of("playerId", playerIdMeta, "name", nameMeta));
    }

    @Test
    void setField_callsHashPutWithEncodedKeyAndValue() {
        accessor.setField(10001L, "playerId", 10001L);
        verify(hashOperations).put(eq("cache:player:10001"), eq("playerId"), anyString());
    }

    @Test
    void setField_name_callsHashPut() {
        accessor.setField(10001L, "name", "testName");
        verify(hashOperations).put(eq("cache:player:10001"), eq("name"), anyString());
    }

    @Test
    void getField_callsHashGetAndDecodes() {
        when(hashOperations.get(eq("cache:player:10001"), eq("playerId"))).thenReturn("10002");
        Long value = accessor.getField(10001L, "playerId", Long.class);
        assertEquals(10002L, value);
        verify(hashOperations).get("cache:player:10001", "playerId");
    }

    @Test
    void getField_null_returnsNull() {
        when(hashOperations.get(anyString(), anyString())).thenReturn(null);
        assertNull(accessor.getField(10001L, "playerId", Long.class));
    }

    @Test
    void setFields_callsHashPutAll() {
        accessor.setFields(10001L, Map.of("playerId", 10001L, "name", "n1"));
        verify(hashOperations).putAll(eq("cache:player:10001"), anyMap());
    }

    @Test
    void getFields_callsMultiGetAndDecodes() {
        when(hashOperations.multiGet(eq("cache:player:10001"), anyList()))
                .thenReturn(List.of("10003", "nameVal"));
        Map<String, Object> result = accessor.getFields(10001L, "playerId", "name");
        assertEquals(2, result.size());
        assertEquals(10003L, result.get("playerId"));
        assertEquals("nameVal", result.get("name"));
    }

    @Test
    void setField_invalidFieldName_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                accessor.setField(10001L, "invalidField", "x"));
    }
}
