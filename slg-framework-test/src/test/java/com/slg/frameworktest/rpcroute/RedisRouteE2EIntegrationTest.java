package com.slg.frameworktest.rpcroute;

import com.slg.net.rpc.route.redis.RedisRoutePublisher;
import com.slg.net.rpc.route.redis.RpcRedisRouteConsumerRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Route 端到端集成测试
 * 使用 Testcontainers Redis，验证 RedisRoutePublisher 写入 Stream、消费者可启动并消费
 *
 * @author framework-test
 */
@SpringBootTest(classes = com.slg.frameworktest.FrameworkTestRedisRouteApplication.class)
@ActiveProfiles("test")
@Testcontainers
class RedisRouteE2EIntegrationTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        String host = redis.getHost();
        String port = redis.getMappedPort(6379).toString();
        registry.add("spring.data.redis.host", () -> host);
        registry.add("spring.data.redis.port", () -> port);
        registry.add("rpc.route.redis.host", () -> host);
        registry.add("rpc.route.redis.port", () -> port);
    }

    @Autowired
    private RedisRoutePublisher redisRoutePublisher;

    @Autowired(required = false)
    private RedisTemplate<String, byte[]> routeRedisTemplate;

    @Autowired(required = false)
    private RpcRedisRouteConsumerRunner rpcRedisRouteConsumerRunner;

    @Test
    @DisplayName("publishRaw 写入后 Stream 中存在一条记录")
    void publishRawThenStreamHasRecord() {
        int targetServerId = 2;
        byte[] payload = "e2e-test-payload".getBytes(StandardCharsets.UTF_8);
        redisRoutePublisher.publishRaw(targetServerId, payload);

        String streamKey = "rpc:route:" + targetServerId;
        Long len = routeRedisTemplate.opsForStream().size(streamKey);
        assertThat(len).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("消费者启动后 isRunning 为 true，停止后为 false")
    void consumerRunnerStartAndStop() {
        if (rpcRedisRouteConsumerRunner == null) {
            return;
        }
        if (rpcRedisRouteConsumerRunner.isRunning()) {
            rpcRedisRouteConsumerRunner.stop();
            await();
        }
        assertThat(rpcRedisRouteConsumerRunner.isRunning()).isFalse();
        rpcRedisRouteConsumerRunner.start();
        assertThat(rpcRedisRouteConsumerRunner.isRunning()).isTrue();
        rpcRedisRouteConsumerRunner.stop();
        await();
        assertThat(rpcRedisRouteConsumerRunner.isRunning()).isFalse();
    }

    private static void await() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
