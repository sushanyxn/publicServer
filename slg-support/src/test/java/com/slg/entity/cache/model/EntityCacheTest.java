package com.slg.entity.cache.model;

import com.slg.common.executor.core.GlobalScheduler;
import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.entity.db.persist.AsyncPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EntityCache 单元测试（Mock AsyncPersistenceService，关闭 Write-Behind）
 */
@ExtendWith(MockitoExtension.class)
class EntityCacheTest {

    @Mock
    private AsyncPersistenceService asyncPersistenceService;

    private EntityCache<EntityCacheTestEntity> cache;

    @BeforeEach
    void setUp() {
        cache = new EntityCache<>(EntityCacheTestEntity.class, asyncPersistenceService, null);
    }

    @Test
    void findById_cacheMiss_callsAsyncPersistenceServiceFindById() {
        EntityCacheTestEntity entity = new EntityCacheTestEntity(1L);
        when(asyncPersistenceService.findById(eq(1L), eq(EntityCacheTestEntity.class)))
                .thenReturn(CompletableFuture.completedFuture(entity));

        EntityCacheTestEntity result = cache.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(asyncPersistenceService).findById(1L, EntityCacheTestEntity.class);
    }

    @Test
    void findById_cacheHit_doesNotCallRepository() {
        EntityCacheTestEntity entity = new EntityCacheTestEntity(2L);
        when(asyncPersistenceService.findById(eq(2L), eq(EntityCacheTestEntity.class)))
                .thenReturn(CompletableFuture.completedFuture(entity));
        cache.findById(2L);

        EntityCacheTestEntity hit = cache.findById(2L);
        assertNotNull(hit);
        assertEquals(2L, hit.getId());
        verify(asyncPersistenceService, times(1)).findById(2L, EntityCacheTestEntity.class);
    }

    @Test
    void findById_nullId_returnsNull() {
        assertNull(cache.findById(null));
        verify(asyncPersistenceService, never()).findById(any(), any());
    }

    @Test
    void save_putsInCacheAndCallsAsyncSave() {
        EntityCacheTestEntity entity = new EntityCacheTestEntity(3L);
        EntityCacheTestEntity result = cache.save(entity);

        assertSame(entity, result);
        assertNotNull(cache.findById(3L));
        verify(asyncPersistenceService).save(entity);
    }

    @Test
    void evict_removesFromCache() {
        EntityCacheTestEntity entity = new EntityCacheTestEntity(4L);
        when(asyncPersistenceService.findById(eq(4L), eq(EntityCacheTestEntity.class)))
                .thenReturn(CompletableFuture.completedFuture(entity));
        cache.findById(4L);
        cache.evict(4L);
        assertNull(cache.getCache().getIfPresent(4L));
    }

    @Test
    void clear_removesAllFromCache() {
        when(asyncPersistenceService.findById(any(), eq(EntityCacheTestEntity.class)))
                .thenReturn(CompletableFuture.completedFuture(new EntityCacheTestEntity(5L)));
        cache.findById(5L);
        cache.clear();
        assertEquals(0, cache.size());
    }

    @CacheConfig(writeDelay = false, autoSaveOnExpire = true)
    public static class EntityCacheTestEntity extends BaseEntity<Long> {
        public EntityCacheTestEntity(Long id) {
            setId(id);
        }
        @Override
        public void save() {}
        @Override
        public void saveField(String fieldName) {}
    }
}
