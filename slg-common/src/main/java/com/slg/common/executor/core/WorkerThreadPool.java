package com.slg.common.executor.core;

import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.slg.common.executor.core.ExecutorConstants.WORKER_POOL_TIMEOUT_SECONDS;
import static com.slg.common.executor.core.ExecutorConstants.WORKER_SYNC_THRESHOLD;

/**
 * 工作线程池
 * 基于虚拟线程的任务执行池，用于并发执行多个任务
 * 当任务数量少于等于 {@link ExecutorConstants#WORKER_SYNC_THRESHOLD} 时同步执行，
 * 超过时使用虚拟线程池并发执行
 *
 * @author yangxunan
 * @date 2026/1/28
 */
@Component
public class WorkerThreadPool {

    /**
     * 共享虚拟线程执行器，由 VirtualExecutorHolder 统一管理
     */
    private ExecutorService virtualExecutor;

    @Autowired
    private VirtualExecutorHolder virtualExecutorHolder;

    @Getter
    private static WorkerThreadPool instance;

    @PostConstruct
    public void init(){
        virtualExecutor = virtualExecutorHolder.getExecutor();
        instance = this;
    }

    public void executeTasks(Collection<Runnable> tasks){
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        if (tasks.size() <= WORKER_SYNC_THRESHOLD) {
            for (Runnable task : tasks) {
                try {
                    task.run();
                } catch (Exception e) {
                    LoggerUtil.error("[工作线程池] 批量执行任务时异常！", e);
                }
            }
            return;
        }

        CountDownLatch latch = new CountDownLatch(tasks.size());

        for (var task : tasks) {
            virtualExecutor.submit(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    LoggerUtil.error("[工作线程池] 批量执行任务时异常！", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            if (!latch.await(WORKER_POOL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LoggerUtil.error("[工作线程池] 批量任务执行超时({}秒)，未完成任务数: {}",
                        WORKER_POOL_TIMEOUT_SECONDS, latch.getCount());
            }
        } catch (Exception e) {
            LoggerUtil.error("[工作线程池] 等待任务执行完成时异常", e);
        }
    }

    @PreDestroy
    public void destroy(){
    }

}
