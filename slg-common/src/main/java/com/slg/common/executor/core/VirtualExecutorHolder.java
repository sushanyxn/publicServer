package com.slg.common.executor.core;

import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.slg.common.executor.core.ExecutorConstants.EXECUTOR_TERMINATION_TIMEOUT_SECONDS;

/**
 * 共享虚拟线程执行器持有者
 * 持有全应用唯一的虚拟线程 {@link ExecutorService}，供各模块统一使用
 *
 * <p>虚拟线程池的职责只是"跑任务"，不负责有序性；
 * 有序性由 {@link KeyedVirtualExecutor} 通过队列 + 消费者模式保证。
 *
 * <p>获取方式：
 * <ul>
 *   <li>Spring 注入：{@code @Autowired VirtualExecutorHolder holder}</li>
 *   <li>静态获取：{@code VirtualExecutorHolder.getInstance()}</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/07
 */
@Component
public class VirtualExecutorHolder {

    /**
     * 虚拟线程执行器
     */
    @Getter
    private ExecutorService executor;

    /**
     * 静态实例，供非 Spring 场景使用
     */
    @Getter
    private static VirtualExecutorHolder instance;

    /**
     * 初始化虚拟线程执行器
     */
    @PostConstruct
    public void init() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        instance = this;
        LoggerUtil.debug("VirtualExecutorHolder 初始化完成");
    }

    /**
     * 销毁虚拟线程执行器
     *
     * <p>虚拟线程池的生命周期由 {@link KeyedVirtualExecutor#destroy()} 统一管理：
     * KVE 会先等待任务完成，超时后主动调用 shutdownNow() 中断阻塞的虚拟线程。
     * 本方法作为兜底，确保即使 KVE 未关闭线程池，此处也能正确关闭。
     */
    @PreDestroy
    public void destroy() {
        LoggerUtil.debug("VirtualExecutorHolder 开始销毁...");

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LoggerUtil.warn("虚拟线程池未能在 {} 秒内完成关闭，强制关闭", EXECUTOR_TERMINATION_TIMEOUT_SECONDS);
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                LoggerUtil.error("等待虚拟线程池关闭时被中断", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } else {
            LoggerUtil.debug("虚拟线程池已由 KeyedVirtualExecutor 关闭，跳过");
        }

        LoggerUtil.debug("VirtualExecutorHolder 已销毁");
    }
}
