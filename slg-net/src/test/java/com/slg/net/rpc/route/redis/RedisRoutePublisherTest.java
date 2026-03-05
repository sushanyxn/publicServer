package com.slg.net.rpc.route.redis;

import com.slg.net.rpc.route.IRpcRouteSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RedisRoutePublisher 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RedisRoutePublisherTest {

    @Mock
    private RedisTemplate<String, byte[]> routeRedisTemplate;

    @Mock
    private IRpcRouteSupportService rpcRouteSupportService;

    private RpcRouteRedisProperties properties;
    private RedisRoutePublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new RpcRouteRedisProperties();
        properties.setStreamMaxLen(1000L);
        publisher = new RedisRoutePublisher(routeRedisTemplate, rpcRouteSupportService, properties);
    }

    @Test
    void publishRaw_callsGetRedisRouteChannelWithTargetServerId() {
        when(rpcRouteSupportService.getRedisRouteChannel(3)).thenReturn("rpc:route:3");
        doReturn(null).when(routeRedisTemplate).execute(any(RedisCallback.class));

        publisher.publishRaw(3, new byte[]{1, 2, 3});

        verify(rpcRouteSupportService).getRedisRouteChannel(3);
        verify(routeRedisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    void publishResp_callsExecute() {
        doReturn(null).when(routeRedisTemplate).execute(any(RedisCallback.class));
        com.slg.net.message.innermessage.rpc.packet.IM_RpcRespone resp = new com.slg.net.message.innermessage.rpc.packet.IM_RpcRespone();
        publisher.publishResp(2, resp);
        verify(routeRedisTemplate).execute(any(RedisCallback.class));
    }
}
