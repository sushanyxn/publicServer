package com.slg.entity.db.persist;

import com.slg.common.log.LoggerUtil;

/**
 * 持久化重试包装器
 * 提取自 PersistenceThreadPool 的重试逻辑，可与 KeyedVirtualExecutor 配合使用
 *
 * <p>执行策略：
 * <ul>
 *   <li>正常执行：直接执行任务</li>
 *   <li>可重试异常：记录警告并重新抛出，由调用方决定是否重试</li>
 *   <li>不可重试异常：记录错误日志</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/09
 */
public class PersistenceRetryWrapper {

    /**
     * 私有构造函数，防止实例化
     */
    private PersistenceRetryWrapper() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 执行持久化任务（带重试逻辑）
     * 如果任务执行失败且异常可重试（且未超过最大重试次数），则重新执行
     *
     * @param task       要执行的持久化任务
     * @param key        任务标识（用于日志）
     * @param maxRetries 最大重试次数
     */
    public static void executeWithRetry(Runnable task, Object key, int maxRetries) {
        int retryCount = 0;
        while (true) {
            try {
                task.run();
                if (retryCount > 0) {
                    LoggerUtil.warn("持久化任务重试成功: key={}, 重试次数={}", key, retryCount);
                }
                return;
            } catch (Exception e) {
                boolean canRetry = PersistenceExceptionUtil.isRetryable(e) && retryCount < maxRetries;
                if (canRetry) {
                    retryCount++;
                    LoggerUtil.warn("持久化任务遇到可重试异常，将重试: key={}, 异常={}, 重试次数={}/{}",
                            key, PersistenceExceptionUtil.getShortDescription(e), retryCount, maxRetries);
                } else {
                    if (retryCount >= maxRetries) {
                        LoggerUtil.error("持久化任务重试失败，已达最大重试次数: key={}, 重试次数={}", key, retryCount, e);
                    } else {
                        LoggerUtil.error("持久化任务执行失败: key={}", key, e);
                    }
                    return;
                }
            }
        }
    }

    /**
     * 执行持久化任务（使用默认最大重试次数）
     *
     * @param task 要执行的持久化任务
     * @param key  任务标识（用于日志）
     */
    public static void executeWithRetry(Runnable task, Object key) {
        executeWithRetry(task, key, PersistenceExceptionUtil.MAX_RETRY_COUNT);
    }
}
