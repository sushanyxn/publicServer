package com.slg.frameworktest.stress;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 持久化框架热点/压力测试：同 key 串行、多 key 并发
 *
 * @author framework-test
 */
@SpringBootTest(classes = com.slg.frameworktest.FrameworkTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class PersistenceStressTest {

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
    @DisplayName("热点：同一实体 ID 高并发 save，不丢不串数据")
    void hotspotSameKeyConcurrentSave() throws Exception {
        long id = System.currentTimeMillis();
        int rounds = 50;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(rounds);
        AtomicInteger completed = new AtomicInteger(0);

        for (int i = 0; i < rounds; i++) {
            final int value = i;
            Thread.ofVirtual().start(() -> {
                try {
                    start.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                TestPersistEntity e = new TestPersistEntity();
                e.setId(id);
                e.setName("round-" + value);
                e.setLevel(value);
                asyncPersistenceService.save(e);
                completed.incrementAndGet();
                done.countDown();
            });
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        assertThat(completed.get()).isEqualTo(rounds);

        Thread.sleep(1000);
        CompletableFuture<TestPersistEntity> f = asyncPersistenceService.findById(id, TestPersistEntity.class);
        TestPersistEntity loaded = f.get(10, TimeUnit.SECONDS);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getLevel()).isBetween(0, rounds - 1);
        assertThat(loaded.getName()).startsWith("round-");
    }

    @Test
    @DisplayName("多 key 压力：不同 ID 并发 insert，全部可查")
    void multiKeyConcurrentInsert() throws Exception {
        int count = 80;
        long baseId = System.currentTimeMillis() * 1000;
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
                TestPersistEntity e = new TestPersistEntity();
                e.setId(baseId + idx);
                e.setName("stress-" + idx);
                e.setLevel(idx);
                asyncPersistenceService.insert(e);
                done.countDown();
            });
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();

        Thread.sleep(2000);
        List<CompletableFuture<TestPersistEntity>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            futures.add(asyncPersistenceService.findById(baseId + i, TestPersistEntity.class));
        }
        for (int i = 0; i < count; i++) {
            TestPersistEntity loaded = futures.get(i).get(10, TimeUnit.SECONDS);
            assertThat(loaded).isNotNull();
            assertThat(loaded.getId()).isEqualTo(baseId + i);
            assertThat(loaded.getName()).isEqualTo("stress-" + i);
            assertThat(loaded.getLevel()).isEqualTo(i);
        }
    }
}
