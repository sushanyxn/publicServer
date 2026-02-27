package com.slg.entity.db.persist;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ConcurrentModificationException;

/**
 * 持久化异常工具类
 * 判断异常是否可重试，与具体数据库实现无关
 *
 * @author yangxunan
 * @date 2025-12-25
 */
public class PersistenceExceptionUtil {

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_COUNT = 3;

    /**
     * 判断异常是否可重试
     * 
     * @param throwable 异常
     * @return true 如果可以重试
     */
    public static boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        // 乐观锁冲突
        if (throwable instanceof OptimisticLockingFailureException) {
            return true;
        }

        // 并发修改异常
        if (throwable instanceof ConcurrentModificationException) {
            return true;
        }

        // 唯一键冲突（通常不应重试）
        if (throwable instanceof DuplicateKeyException) {
            return false;
        }

        // 通过类名判断数据库特有的可重试异常，避免硬依赖具体数据库驱动
        String className = throwable.getClass().getName();
        if (className.contains("MongoWriteConcernException")) {
            return true;
        }
        if (className.contains("DeadlockLoserDataAccessException")
                || className.contains("CannotAcquireLockException")) {
            return true;
        }
        // JPA 锁超时、悲观锁异常
        if (className.contains("LockTimeoutException")
                || className.contains("PessimisticLockException")) {
            return true;
        }

        // 检查 cause
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return isRetryable(cause);
        }

        return false;
    }

    /**
     * 获取异常的简短描述
     * 
     * @param throwable 异常
     * @return 简短描述
     */
    public static String getShortDescription(Throwable throwable) {
        if (throwable == null) {
            return "Unknown";
        }
        String className = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        if (message != null && message.length() > 50) {
            message = message.substring(0, 50) + "...";
        }
        return className + ": " + message;
    }
}


