package com.slg.frameworktest.performance;

import com.slg.redis.service.RedisCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 缓存框架性能测试：吞吐与延迟
 *
 * @author framework-test
 */
@SpringBootTest(classes = com.slg.frameworktest.FrameworkTestRedisOnlyApplication.class)
@ActiveProfiles("test")
@Testcontainers
class RedisCachePerformanceTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private RedisCacheService redisCacheService;

    @Test
    @DisplayName("set 吞吐：1000 次/秒量级可接受")
    void setThroughput() {
        int count = 2000;
        String prefix = "perf:set:" + System.currentTimeMillis() + ":";
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            redisCacheService.set(prefix + i, "v" + i);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        double opsPerSec = count * 1000.0 / Math.max(1, elapsedMs);
        System.out.printf("[Redis] set %d 次, 耗时 %d ms, 约 %.0f 次/秒%n", count, elapsedMs, opsPerSec);

        Object v = redisCacheService.get(prefix + (count - 1));
        assertThat(v).isEqualTo("v" + (count - 1));
    }

    @Test
    @DisplayName("get 吞吐与延迟：1000 次/秒、单次 10ms 内")
    void getThroughputAndLatency() {
        String key = "perf:get:latency:" + System.currentTimeMillis();
        redisCacheService.set(key, "value");

        int count = 1000;
        List<Long> latencies = new ArrayList<>(count);
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            long t0 = System.nanoTime();
            redisCacheService.get(key);
            latencies.add((System.nanoTime() - t0) / 1_000);
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;
        double opsPerSec = count * 1000.0 / Math.max(1, totalMs);
        latencies.sort(Long::compareTo);
        long p99 = latencies.get((int) (count * 0.99));
        System.out.printf("[Redis] get %d 次, 总耗时 %d ms, 约 %.0f 次/秒, P99 延迟 %d μs%n", count, totalMs, opsPerSec, p99);

        assertThat(redisCacheService.get(key)).isEqualTo("value");
        assertThat(p99).isLessThan(10_000); // P99 < 10ms
    }
}
