package com.slg.frameworktest.persistence;

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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 持久化框架端到端集成测试
 * 使用 Testcontainers MySQL + Redis，验证 AsyncPersistenceService + BaseMysqlRepository 真实落库与查询
 *
 * @author framework-test
 */
@SpringBootTest(classes = com.slg.frameworktest.FrameworkTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class PersistenceE2EIntegrationTest {

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
    @DisplayName("insert 后 findById 能查到且数据一致")
    void insertThenFindById() throws Exception {
        long id = System.currentTimeMillis();
        TestPersistEntity entity = new TestPersistEntity();
        entity.setId(id);
        entity.setName("e2e-" + id);
        entity.setLevel(10);

        asyncPersistenceService.insert(entity);

        CompletableFuture<TestPersistEntity> future = asyncPersistenceService.findById(id, TestPersistEntity.class);
        TestPersistEntity loaded = future.get(5, TimeUnit.SECONDS);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo(id);
        assertThat(loaded.getName()).isEqualTo(entity.getName());
        assertThat(loaded.getLevel()).isEqualTo(10);
    }

    @Test
    @DisplayName("save 更新后 findById 得到最新数据")
    void saveThenFindById() throws Exception {
        long id = System.currentTimeMillis() + 1;
        TestPersistEntity entity = new TestPersistEntity();
        entity.setId(id);
        entity.setName("first");
        entity.setLevel(1);
        asyncPersistenceService.insert(entity);

        entity.setName("updated");
        entity.setLevel(2);
        asyncPersistenceService.save(entity);

        CompletableFuture<TestPersistEntity> future = asyncPersistenceService.findById(id, TestPersistEntity.class);
        TestPersistEntity loaded = future.get(5, TimeUnit.SECONDS);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("updated");
        assertThat(loaded.getLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateField 后 findById 字段已更新")
    void updateFieldThenFindById() throws Exception {
        long id = System.currentTimeMillis() + 2;
        TestPersistEntity entity = new TestPersistEntity();
        entity.setId(id);
        entity.setName("before");
        entity.setLevel(5);
        asyncPersistenceService.insert(entity);

        asyncPersistenceService.updateField(id, "name", "afterField", TestPersistEntity.class);
        asyncPersistenceService.updateField(id, "level", 99, TestPersistEntity.class);

        // 等待持久化队列执行
        Thread.sleep(500);

        CompletableFuture<TestPersistEntity> future = asyncPersistenceService.findById(id, TestPersistEntity.class);
        TestPersistEntity loaded = future.get(5, TimeUnit.SECONDS);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("afterField");
        assertThat(loaded.getLevel()).isEqualTo(99);
    }

    @Test
    @DisplayName("deleteById 软删后 findById 返回 null")
    void deleteByIdThenFindByIdReturnsNull() throws Exception {
        long id = System.currentTimeMillis() + 3;
        TestPersistEntity entity = new TestPersistEntity();
        entity.setId(id);
        entity.setName("toDelete");
        entity.setLevel(0);
        asyncPersistenceService.insert(entity);

        asyncPersistenceService.deleteById(id, TestPersistEntity.class);
        Thread.sleep(500);

        CompletableFuture<TestPersistEntity> future = asyncPersistenceService.findById(id, TestPersistEntity.class);
        TestPersistEntity loaded = future.get(5, TimeUnit.SECONDS);
        assertThat(loaded).isNull();
    }
}
