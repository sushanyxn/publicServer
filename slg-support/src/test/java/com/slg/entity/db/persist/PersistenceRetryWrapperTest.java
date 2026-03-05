package com.slg.entity.db.persist;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PersistenceRetryWrapper 单元测试
 */
class PersistenceRetryWrapperTest {

    @Test
    void executeWithRetry_successFirstTime_doesNotRetry() {
        AtomicInteger runs = new AtomicInteger(0);
        PersistenceRetryWrapper.executeWithRetry(runs::incrementAndGet, "key1", 3);
        assertEquals(1, runs.get());
    }

    @Test
    void executeWithRetry_retryableThenSuccess_retriesUntilSuccess() {
        AtomicInteger runs = new AtomicInteger(0);
        PersistenceRetryWrapper.executeWithRetry(() -> {
            if (runs.incrementAndGet() < 2) {
                throw new OptimisticLockingFailureException("conflict");
            }
        }, "key2", 3);
        assertEquals(2, runs.get());
    }

    @Test
    void executeWithRetry_retryableUntilMaxRetries_thenStops() {
        AtomicInteger runs = new AtomicInteger(0);
        int maxRetries = 2;
        PersistenceRetryWrapper.executeWithRetry(() -> {
            runs.incrementAndGet();
            throw new OptimisticLockingFailureException("always");
        }, "key3", maxRetries);
        assertEquals(maxRetries + 1, runs.get());
    }

    @Test
    void executeWithRetry_nonRetryableException_doesNotRetry() {
        AtomicInteger runs = new AtomicInteger(0);
        PersistenceRetryWrapper.executeWithRetry(() -> {
            runs.incrementAndGet();
            throw new IllegalArgumentException("bad");
        }, "key4", 3);
        assertEquals(1, runs.get());
    }

    @Test
    void executeWithRetry_defaultMaxRetries_usesConstant() {
        AtomicInteger runs = new AtomicInteger(0);
        PersistenceRetryWrapper.executeWithRetry(() -> {
            runs.incrementAndGet();
            throw new OptimisticLockingFailureException("x");
        }, "key5");
        assertEquals(PersistenceExceptionUtil.MAX_RETRY_COUNT + 1, runs.get());
    }
}
