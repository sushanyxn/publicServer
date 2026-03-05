package com.slg.frameworktest.stress;

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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 缓存热点/压力测试：同 key 高并发、多 key 高并发
 *
 * @author framework-test
 */
@SpringBootTest(classes = com.slg.frameworktest.FrameworkTestRedisOnlyApplication.class)
@ActiveProfiles("test")
@Testcontainers
class RedisCacheStressTest {

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
    @DisplayName("热点 key：同一 key 高并发 set/get，最终值一致")
    void hotspotSameKeyConcurrentSetGet() throws Exception {
        String key = "stress:hotspot:" + System.currentTimeMillis();
        int threads = 50;
        int opsPerThread = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger setCount = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            Thread.ofVirtual().start(() -> {
                try {
                    start.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < opsPerThread; i++) {
                    redisCacheService.set(key, "t" + threadId + "-" + i);
                    setCount.incrementAndGet();
                    redisCacheService.get(key);
                }
                done.countDown();
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();

        Object finalVal = redisCacheService.get(key);
        assertThat(finalVal).isNotNull();
        assertThat(finalVal.toString()).startsWith("t");
    }

    @Test
    @DisplayName("多 key 压力：大量不同 key 并发 set/get")
    void multiKeyConcurrentSetGet() throws Exception {
        String prefix = "stress:multi:" + System.currentTimeMillis() + ":";
        int count = 200;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            final int idx = i;
            Thread.ofVirtual().start(() -> {
                try {
                    start.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                String k = prefix + idx;
                redisCacheService.set(k, "v" + idx);
                Object v = redisCacheService.get(k);
                assertThat(v).isEqualTo("v" + idx);
                done.countDown();
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();

        for (int i = 0; i < count; i++) {
            assertThat(redisCacheService.get(prefix + i)).isEqualTo("v" + i);
        }
    }
}
