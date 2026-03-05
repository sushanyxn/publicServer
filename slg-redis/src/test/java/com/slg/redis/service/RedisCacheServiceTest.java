package com.slg.redis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * RedisCacheService 单元测试（Mock RedisTemplate）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ValueOperations<String, String> stringValueOps;

    @InjectMocks
    private RedisCacheService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOps);
    }

    @Test
    void set_callsRedisTemplateOpsForValueSet() {
        service.set("k1", "v1");
        verify(valueOps).set(eq("k1"), eq("v1"));
    }

    @Test
    void set_withTimeout_callsSetWithTimeout() {
        service.set("k2", "v2", 10, TimeUnit.SECONDS);
        verify(valueOps).set(eq("k2"), eq("v2"), eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    void get_callsRedisTemplateOpsForValueGet() {
        when(valueOps.get("k3")).thenReturn("v3");
        assertEquals("v3", service.get("k3"));
        verify(valueOps).get("k3");
    }

    @Test
    void get_null_returnsNull() {
        when(valueOps.get("missing")).thenReturn(null);
        assertNull(service.get("missing"));
    }

    @Test
    void delete_callsRedisTemplateDelete() {
        when(redisTemplate.delete(anyString())).thenReturn(Boolean.TRUE);
        service.delete("k4");
        verify(redisTemplate).delete("k4");
    }

    @Test
    void setString_callsStringRedisTemplateSet() {
        service.setString("sk1", "sv1");
        verify(stringValueOps).set(eq("sk1"), eq("sv1"));
    }

    @Test
    void getString_callsStringRedisTemplateGet() {
        when(stringValueOps.get("sk2")).thenReturn("sv2");
        assertEquals("sv2", service.getString("sk2"));
        verify(stringValueOps).get("sk2");
    }
}
