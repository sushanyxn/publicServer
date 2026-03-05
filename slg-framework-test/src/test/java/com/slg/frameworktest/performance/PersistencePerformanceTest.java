package com.slg.frameworktest.performance;

import com.slg.entity.db.persist.AsyncPersistenceService;
import com.slg.frameworktest.persistence.entity.TestPersistEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 持久化框架性能测试：吞吐与延迟
 *
 * @author framework-test
 */
@SpringBootTest(classes = com.slg.frameworktest.FrameworkTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class PersistencePerformanceTest {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("slg_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private AsyncPersistenceService asyncPersistenceService;

    @Test
    @DisplayName("批量 insert 吞吐：100 条/秒量级可接受")
    void insertThroughput() throws Exception {
        int count = 100;
        long baseId = System.currentTimeMillis() * 1000;
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            TestPersistEntity e = new TestPersistEntity();
            e.setId(baseId + i);
            e.setName("perf-" + i);
            e.setLevel(i);
            asyncPersistenceService.insert(e);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        double opsPerSec = count * 1000.0 / Math.max(1, elapsedMs);
        System.out.printf("[Persistence] insert %d 条, 提交耗时 %d ms, 约 %.0f 条/秒（仅提交到队列）%n", count, elapsedMs, opsPerSec);

        CompletableFuture<TestPersistEntity> last = asyncPersistenceService.findById(baseId + count - 1, TestPersistEntity.class);
        TestPersistEntity loaded = last.get(30, TimeUnit.SECONDS);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("perf-" + (count - 1));
    }

    @Test
    @DisplayName("findById 延迟：单次 get(5s) 内完成")
    void findByIdLatency() throws Exception {
        long id = System.currentTimeMillis();
        TestPersistEntity e = new TestPersistEntity();
        e.setId(id);
        e.setName("latency");
        e.setLevel(1);
        asyncPersistenceService.insert(e);

        long start = System.nanoTime();
        CompletableFuture<TestPersistEntity> future = asyncPersistenceService.findById(id, TestPersistEntity.class);
        TestPersistEntity loaded = future.get(5, TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("[Persistence] findById 延迟约 %d ms%n", elapsedMs);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo(id);
    }
}
