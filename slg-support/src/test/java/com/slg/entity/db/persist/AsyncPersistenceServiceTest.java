package com.slg.entity.db.persist;

import com.slg.entity.db.entity.BaseEntity;
import com.slg.entity.db.repository.BaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AsyncPersistenceService 单元测试（不依赖 Executor 的用例）
 */
@ExtendWith(MockitoExtension.class)
class AsyncPersistenceServiceTest {

    @Mock
    private BaseRepository repository;

    @InjectMocks
    private AsyncPersistenceService service;

    @BeforeEach
    void setUp() {
        // 确保 Mock 注入
    }

    @Test
    void save_nullEntity_doesNotCallRepository() {
        service.save(null);
        verify(repository, never()).save(any());
    }

    @Test
    void save_entityWithNullId_doesNotCallRepository() {
        BaseEntity<Long> entity = new TestEntity(null);
        service.save(entity);
        verify(repository, never()).save(any());
    }

    @Test
    void findById_nullId_returnsCompletedFutureWithNull() throws ExecutionException, InterruptedException {
        CompletableFuture<TestEntity> future = service.findById(null, TestEntity.class);
        assertNotNull(future);
        assertNull(future.get());
        verify(repository, never()).findById(any(), any(Class.class));
    }

    @Test
    void findAll_nullEntityClass_returnsCompletedFutureWithEmptyList() throws ExecutionException, InterruptedException {
        CompletableFuture<java.util.List<TestEntity>> future = service.findAll(null);
        assertNotNull(future);
        assertTrue(future.get().isEmpty());
        verify(repository, never()).findAll(any(Class.class));
    }

    /** 测试用实体，id 可置为 null */
    public static class TestEntity extends BaseEntity<Long> {
        public TestEntity(Long id) {
            setId(id);
        }
        @Override
        public void save() {}
        @Override
        public void saveField(String fieldName) {}
    }
}
