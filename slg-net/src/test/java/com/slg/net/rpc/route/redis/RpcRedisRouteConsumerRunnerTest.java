package com.slg.net.rpc.route.redis;

import com.slg.net.rpc.facade.RpcRedisFacade;
import com.slg.net.rpc.route.IRpcRouteSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * RpcRedisRouteConsumerRunner 单元测试（启动/停止状态与 getPhase）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RpcRedisRouteConsumerRunnerTest {

    @Mock
    private RedisTemplate<String, byte[]> routeRedisTemplate;

    @Mock
    private RpcRedisFacade rpcRedisFacade;

    @Mock
    private IRpcRouteSupportService rpcRouteSupportService;

    private RpcRouteRedisProperties properties;
    private RpcRedisRouteConsumerRunner runner;

    @BeforeEach
    void setUp() {
        properties = new RpcRouteRedisProperties();
        when(rpcRouteSupportService.getLocalRedisRouteChannel()).thenReturn("rpc:route:1");
        when(rpcRouteSupportService.getLocalRedisRespChannel()).thenReturn("rpc:route:resp:1");
        when(rpcRouteSupportService.getLocalServerId()).thenReturn(1);
        runner = new RpcRedisRouteConsumerRunner(routeRedisTemplate, rpcRedisFacade, rpcRouteSupportService, properties);
    }

    @Test
    void beforeStart_isRunning_returnsFalse() {
        assertFalse(runner.isRunning());
    }

    @Test
    void getPhase_returnsHighPhase() {
        assertTrue(runner.getPhase() > 0);
    }

    @Test
    void start_setsRunningToTrue() {
        runner.start();
        assertTrue(runner.isRunning());
        runner.stop();
    }

    @Test
    void stop_setsRunningToFalse() {
        runner.start();
        runner.stop();
        assertFalse(runner.isRunning());
    }
}
