package com.slg.entity.db.persist;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ConcurrentModificationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PersistenceExceptionUtil 单元测试
 */
class PersistenceExceptionUtilTest {

    @Test
    void isRetryable_null_returnsFalse() {
        assertFalse(PersistenceExceptionUtil.isRetryable(null));
    }

    @Test
    void isRetryable_OptimisticLockingFailureException_returnsTrue() {
        assertTrue(PersistenceExceptionUtil.isRetryable(new OptimisticLockingFailureException("conflict")));
    }

    @Test
    void isRetryable_ConcurrentModificationException_returnsTrue() {
        assertTrue(PersistenceExceptionUtil.isRetryable(new ConcurrentModificationException()));
    }

    @Test
    void isRetryable_DuplicateKeyException_returnsFalse() {
        assertFalse(PersistenceExceptionUtil.isRetryable(new DuplicateKeyException("dup")));
    }

    @Test
    void isRetryable_causeChain_retryableCause_returnsTrue() {
        Exception cause = new OptimisticLockingFailureException("cause");
        Exception top = new RuntimeException("top", cause);
        assertTrue(PersistenceExceptionUtil.isRetryable(top));
    }

    @Test
    void isRetryable_causeChain_nonRetryableCause_returnsFalse() {
        Exception cause = new DuplicateKeyException("dup");
        Exception top = new RuntimeException("top", cause);
        assertFalse(PersistenceExceptionUtil.isRetryable(top));
    }

    @Test
    void isRetryable_ordinaryException_returnsFalse() {
        assertFalse(PersistenceExceptionUtil.isRetryable(new IllegalArgumentException("arg")));
    }

    @Test
    void getShortDescription_null_returnsUnknown() {
        assertEquals("Unknown", PersistenceExceptionUtil.getShortDescription(null));
    }

    @Test
    void getShortDescription_withShortMessage_returnsClassNameAndMessage() {
        String msg = PersistenceExceptionUtil.getShortDescription(new IllegalStateException("short"));
        assertTrue(msg.startsWith("IllegalStateException:"));
        assertTrue(msg.contains("short"));
    }

    @Test
    void getShortDescription_withLongMessage_truncatesTo50() {
        String longMsg = "a".repeat(60);
        String result = PersistenceExceptionUtil.getShortDescription(new Exception(longMsg));
        assertTrue(result.contains("..."));
    }

    @Test
    void maxRetryCount_constant() {
        assertTrue(PersistenceExceptionUtil.MAX_RETRY_COUNT >= 1);
    }
}
