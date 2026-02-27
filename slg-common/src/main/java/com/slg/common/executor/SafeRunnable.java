package com.slg.common.executor;

import com.slg.common.log.LoggerUtil;

/**
 * 安全的Runnable包装类
 * 统一处理任务执行时的异常，避免异常导致线程终止
 *
 * @param task 实际要执行的任务
 * @author yangxunan
 * @date 2026/01/21
 */
public record SafeRunnable(Runnable task) implements Runnable{

    @Override
    public void run(){
        try {
            task.run();
        } catch (Throwable e) {
            LoggerUtil.error("执行任务异常", e);
        }
    }

    /**
     * 创建SafeRunnable实例
     *
     * @param task 要执行的任务
     * @return SafeRunnable实例
     */
    public static SafeRunnable wrap(Runnable task){
        return new SafeRunnable(task);
    }
}

