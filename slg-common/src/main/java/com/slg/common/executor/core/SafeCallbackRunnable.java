package com.slg.common.executor.core;

import com.slg.common.log.LoggerUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * 带CompletableFuture的Callable包装类
 * 将Callable和CompletableFuture包装成Runnable，可以提交给线程池执行
 * 执行Callable后自动将结果或异常设置到CompletableFuture中
 *
 * @param <T> 任务返回值类型
 * @param task 实际要执行的任务
 * @param future 用于接收结果的CompletableFuture
 * @author yangxunan
 * @date 2026/01/27
 */
public record SafeCallbackRunnable<T>(Callable<T> task, CompletableFuture<T> future) implements Runnable {

    @Override
    public void run() {
        try {
            T result = task.call();
            future.complete(result);
        } catch (Throwable e) {
            LoggerUtil.error("执行任务异常", e);
            future.completeExceptionally(e);
        }
    }

    /**
     * 创建CallbackRunnable实例
     *
     * @param task 要执行的任务
     * @param future 用于接收结果的CompletableFuture
     * @param <T> 返回值类型
     * @return CallbackRunnable实例
     */
    public static <T> SafeCallbackRunnable<T> wrap(Callable<T> task, CompletableFuture<T> future) {
        return new SafeCallbackRunnable<>(task, future);
    }

    /**
     * 创建CallbackRunnable实例（Runnable版本，无返回值）
     *
     * @param task 要执行的任务
     * @param future 用于接收结果的CompletableFuture
     * @return CallbackRunnable实例
     */
    public static SafeCallbackRunnable<Void> wrap(Runnable task, CompletableFuture<Void> future) {
        return new SafeCallbackRunnable<>(() -> {
            task.run();
            return null;
        }, future);
    }
}
