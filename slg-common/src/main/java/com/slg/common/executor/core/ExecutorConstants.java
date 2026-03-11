package com.slg.common.executor.core;

/**
 * 执行器相关常量
 * 统一管理 {@link KeyedVirtualExecutor}、{@link GlobalScheduler}、{@link WorkerThreadPool} 等组件的配置常量
 *
 * @author yangxunan
 * @date 2026/03/11
 */
public final class ExecutorConstants {

    private ExecutorConstants() {
    }

    // ======================== 队列容量 ========================

    /**
     * 单链模块默认队列容量
     */
    public static final int DEFAULT_SINGLE_CHAIN_QUEUE_SIZE = 5000;

    /**
     * 多链模块默认队列容量
     */
    public static final int DEFAULT_MULTI_CHAIN_QUEUE_SIZE = 1000;

    // ======================== 链标识 ========================

    /**
     * 单链模式的 id 值（无 ID）
     */
    public static final long SINGLE_CHAIN_ID = 0L;

    // ======================== 调度器 ========================

    /**
     * 平台调度线程数
     * 调度器仅做"到点触发 + 转投虚拟线程"，2 个线程足够
     */
    public static final int SCHEDULER_THREAD_COUNT = 2;

    /**
     * 空闲队列清理间隔（分钟）
     */
    public static final int CLEANUP_IDLE_INTERVAL_MINUTES = 5;

    // ======================== Watchdog ========================

    /**
     * Watchdog 扫描间隔（秒）
     */
    public static final int WATCHDOG_SCAN_INTERVAL_SECONDS = 10;

    /**
     * 任务执行告警阈值（毫秒），超过此时间记录 WARN 日志并输出线程栈
     */
    public static final long WATCHDOG_WARN_THRESHOLD_MS = 30_000;

    /**
     * 任务执行强制中断阈值（毫秒），超过此时间中断消费者虚拟线程
     */
    public static final long WATCHDOG_INTERRUPT_THRESHOLD_MS = 300_000;

    // ======================== 关闭 ========================

    /**
     * 等待所有任务完成的超时时间（毫秒）
     */
    public static final long SHUTDOWN_TIMEOUT_MS = 60_000;

    /**
     * 虚拟线程池强制关闭后等待终止的时间（秒）
     */
    public static final int EXECUTOR_TERMINATION_TIMEOUT_SECONDS = 5;

    // ======================== WorkerThreadPool ========================

    /**
     * 工作线程池批量任务超时时间（秒）
     */
    public static final int WORKER_POOL_TIMEOUT_SECONDS = 300;

    /**
     * 同步执行的任务数阈值，小于等于此值直接同步执行
     */
    public static final int WORKER_SYNC_THRESHOLD = 3;

    // ======================== scheduleAtFixedRate ========================

    /**
     * 队列积压阈值，超过此值 scheduleAtFixedRate 跳过本次投递
     */
    public static final int ACCUMULATION_THRESHOLD = 100;
}
