package com.slg.frameworktest.redis;

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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 缓存框架端到端集成测试
 * 使用 Testcontainers Redis，验证 RedisCacheService set/get/delete/过期
 *
 * @author framework-test
 */
@SpringBootTest(classes = com.slg.frameworktest.FrameworkTestRedisOnlyApplication.class)
@ActiveProfiles("test")
@Testcontainers
class RedisCacheE2EIntegrationTest {

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
    @DisplayName("set 后 get 能取回相同值")
    void setThenGet() {
        String key = "e2e:key:" + System.currentTimeMillis();
        String value = "value-" + System.currentTimeMillis();
        redisCacheService.set(key, value);
        Object got = redisCacheService.get(key);
        assertThat(got).isEqualTo(value);
    }

    @Test
    @DisplayName("get 指定类型返回正确类型")
    void getWithClass() {
        String key = "e2e:typed:" + System.currentTimeMillis();
        redisCacheService.set(key, 100);
        Integer v = redisCacheService.get(key, Integer.class);
        assertThat(v).isEqualTo(100);
    }

    @Test
    @DisplayName("delete 后 get 返回 null")
    void deleteThenGetNull() {
        String key = "e2e:del:" + System.currentTimeMillis();
        redisCacheService.set(key, "x");
        redisCacheService.delete(key);
        assertThat(redisCacheService.get(key)).isNull();
    }

    @Test
    @DisplayName("带过期时间的 set 到期后 get 返回 null")
    void setWithExpireThenGetNull() throws InterruptedException {
        String key = "e2e:expire:" + System.currentTimeMillis();
        redisCacheService.set(key, "short", 1, TimeUnit.SECONDS);
        assertThat(redisCacheService.get(key)).isEqualTo("short");
        Thread.sleep(1500);
        assertThat(redisCacheService.get(key)).isNull();
    }
}
