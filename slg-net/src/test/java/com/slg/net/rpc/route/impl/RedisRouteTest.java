package com.slg.net.rpc.route.impl;

import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.route.redis.RedisRoutePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * RedisRoute 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RedisRouteTest {

    @Mock
    private RedisRoutePublisher redisRoutePublisher;

    @InjectMocks
    private RedisRoute redisRoute;

    @BeforeEach
    void setUp() {
        // routeSupportService 由 Mockito 注入为 null，isLocal 等可能 NPE，仅测 getRouteParams 与 sendMsg 参数校验
    }

    @Test
    void getRouteParams_returnsIntClassArray() {
        Class<?>[] params = redisRoute.getRouteParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(int.class, params[0]);
    }

    @Test
    void sendMsg_nullParams_throwsException() {
        IM_RpcRequest request = new IM_RpcRequest();
        RpcMethodMeta meta = new RpcMethodMeta();
        assertThrows(IllegalArgumentException.class, () ->
                redisRoute.sendMsg(request, meta, (Object[]) null));
    }

    @Test
    void sendMsg_emptyParams_throwsException() {
        IM_RpcRequest request = new IM_RpcRequest();
        RpcMethodMeta meta = new RpcMethodMeta();
        assertThrows(IllegalArgumentException.class, () ->
                redisRoute.sendMsg(request, meta));
    }

    @Test
    void sendMsg_validParams_callsPublisher() {
        IM_RpcRequest request = new IM_RpcRequest();
        RpcMethodMeta meta = new RpcMethodMeta();
        redisRoute.sendMsg(request, meta, 2);
        verify(redisRoutePublisher).publish(eq(2), eq(request));
    }

    @Test
    void resolveTargetServerId_returnsFirstParam() {
        assertEquals(5, redisRoute.resolveTargetServerId(null, 5));
        assertEquals(0, redisRoute.resolveTargetServerId(null));
    }
}
