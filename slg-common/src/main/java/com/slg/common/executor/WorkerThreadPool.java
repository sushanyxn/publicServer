package com.slg.common.executor;

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

/**
 * 工作线程池
 * 基于虚拟线程的任务执行池，用于并发执行多个任务
 * 当任务数量少于等于3时同步执行，超过3个时使用虚拟线程池并发执行
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

        if (tasks.size() <= 3) {
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
            latch.await();
        } catch (Exception e) {
            LoggerUtil.error("[工作线程池] 等待任务执行完成时异常", e);
        }
    }

    @PreDestroy
    public void destroy(){
        // 虚拟线程执行器由 VirtualExecutorHolder 统一管理生命周期，此处不再关闭
    }

}
