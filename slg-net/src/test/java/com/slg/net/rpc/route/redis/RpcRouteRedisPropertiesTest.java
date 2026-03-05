package com.slg.net.rpc.route.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RpcRouteRedisProperties 单元测试（默认值与类型）
 */
class RpcRouteRedisPropertiesTest {

    @Test
    void defaultValues() {
        RpcRouteRedisProperties p = new RpcRouteRedisProperties();
        assertEquals("localhost", p.getHost());
        assertEquals(6380, p.getPort());
        assertEquals(0, p.getDatabase());
        assertEquals(3000, p.getTimeout());
        assertEquals("rpc-route-group", p.getConsumerGroup());
        assertEquals(10, p.getBatchSize());
        assertEquals(1, p.getBlockSeconds());
        assertTrue(p.getStreamMaxLen() > 0);
    }

    @Test
    void settersAndGetters() {
        RpcRouteRedisProperties p = new RpcRouteRedisProperties();
        p.setHost("redis.example.com");
        p.setPort(6390);
        p.setStreamMaxLen(20000L);
        assertEquals("redis.example.com", p.getHost());
        assertEquals(6390, p.getPort());
        assertEquals(20000L, p.getStreamMaxLen());
    }
}
