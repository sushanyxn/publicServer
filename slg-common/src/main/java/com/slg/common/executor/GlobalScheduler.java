package com.slg.common.executor;

import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 全局定时调度器
 * 持有全应用共享的 {@link ScheduledExecutorService}（平台线程），用于定时/周期任务的触发
 *
 * <p>设计理念：定时线程仅负责"到点触发"，到点后自动将任务提交到
 * {@link KeyedVirtualExecutor} 按 key 有序执行，定时线程本身完全不做业务逻辑。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 多链：5 秒后在玩家虚拟线程中执行（按 playerId 有序）
 * scheduler.schedule(TaskModule.PLAYER, playerId, () -> {
 *     // 玩家业务逻辑
 * }, 5, TimeUnit.SECONDS);
 *
 * // 单链：每 10 秒在场景虚拟线程中执行
 * scheduler.scheduleWithFixedDelay(TaskModule.SCENE, () -> {
 *     // 场景业务逻辑
 * }, 0, 10, TimeUnit.SECONDS);
 * }</pre>
 *
 * <p>获取方式：
 * <ul>
 *   <li>Spring 注入：{@code @Autowired GlobalScheduler scheduler}</li>
 *   <li>静态获取：{@code GlobalScheduler.getInstance()}</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/07
 */
@Component
public class GlobalScheduler {

    /**
     * 调度线程数（平台线程）
     * 调度器仅做"到点触发 + 转投虚拟线程"，2 个线程足够
     */
    private static final int SCHEDULER_THREAD_COUNT = 2;

    /**
     * 定时调度线程池
     */
    @Getter
    private ScheduledExecutorService scheduler;

    /**
     * 静态实例，供非 Spring 场景使用
     */
    @Getter
    private static GlobalScheduler instance;

    /**
     * 线程命名计数器
     */
    private final AtomicInteger threadCounter = new AtomicInteger(0);

    @Autowired
    private KeyedVirtualExecutor keyedVirtualExecutor;

    /**
     * 检查调度器是否已关闭
     * 关闭后不再接受新任务，直接返回 null，避免 RejectedExecutionException
     *
     * @return true 表示已关闭，调用方应直接返回 null
     */
    private boolean isShutdown() {
        if (scheduler == null || scheduler.isShutdown()) {
            LoggerUtil.debug("GlobalScheduler 已关闭，忽略调度请求");
            return true;
        }
        return false;
    }

    /**
     * 初始化定时调度器
     */
    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(SCHEDULER_THREAD_COUNT, r -> {
            Thread thread = new Thread(r, "scheduler-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        instance = this;

        // 注册 KeyedVirtualExecutor 空闲队列定期清理（每 5 分钟）
        scheduler.scheduleWithFixedDelay(
                () -> keyedVirtualExecutor.cleanupIdleQueues(),
                5, 5, TimeUnit.MINUTES
        );

        LoggerUtil.debug("GlobalScheduler 初始化完成, 线程数={}", SCHEDULER_THREAD_COUNT);
    }

    // ======================== 多链延迟执行（模块 + ID） ========================

    /**
     * 延迟执行一次性任务（多链：模块 + ID）
     * 到点后自动提交到 {@link KeyedVirtualExecutor} 按 key 有序执行
     *
     * @param module 模块枚举
     * @param id     标识（如 playerId）
     * @param task   要执行的业务任务
     * @param delay  延迟时间
     * @param unit   时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> schedule(TaskModule module, long id, Runnable task, long delay, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        return scheduler.schedule(() -> keyedVirtualExecutor.execute(module, id, task), delay, unit);
    }

    /**
     * 以固定速率周期执行任务（多链：模块 + ID）
     * 每次到点后自动提交到 {@link KeyedVirtualExecutor} 按 key 有序执行
     *
     * @param module       模块枚举
     * @param id           标识（如 playerId）
     * @param task         要执行的业务任务
     * @param initialDelay 初始延迟时间
     * @param period       周期间隔
     * @param unit         时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(TaskModule module, long id, Runnable task,
                                                  long initialDelay, long period, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        return scheduler.scheduleAtFixedRate(() -> keyedVirtualExecutor.execute(module, id, task),
                initialDelay, period, unit);
    }

    /**
     * 以固定延迟周期执行任务（多链：模块 + ID）
     * 保证上一次任务在虚拟线程中执行完毕后，才开始计时下一次延迟
     *
     * <p>实现方式：使用 {@link KeyedVirtualExecutor#submit} 提交任务获取 {@link CompletableFuture}，
     * 在 {@code whenComplete} 回调中自递归调度下一次，从而精确控制延迟起点。
     * 返回 {@link CancellableScheduledFuture}，调用 {@code cancel()} 可终止整个周期调度链。
     *
     * @param module       模块枚举
     * @param id           标识（如 playerId）
     * @param task         要执行的业务任务
     * @param initialDelay 初始延迟时间
     * @param delay        固定延迟时间
     * @param unit         时间单位
     * @return CancellableScheduledFuture，可用于取消整个周期调度
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(TaskModule module, long id, Runnable task,
                                                     long initialDelay, long delay, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        Runnable scheduleOnce = new Runnable() {
            @Override
            public void run() {
                CompletableFuture<Void> future = keyedVirtualExecutor.submit(module, id, task);
                future.whenComplete((v, ex) -> {
                    if (!cancelled.get() && !isShutdown()) {
                        futureRef.set(scheduler.schedule(this, delay, unit));
                    }
                });
            }
        };

        futureRef.set(scheduler.schedule(scheduleOnce, initialDelay, unit));
        return new CancellableScheduledFuture(futureRef, cancelled);
    }

    // ======================== 单链延迟执行（仅模块） ========================

    /**
     * 延迟执行一次性任务（单链：仅模块）
     * 到点后自动提交到 {@link KeyedVirtualExecutor} 按模块有序执行
     *
     * @param module 模块枚举
     * @param task   要执行的业务任务
     * @param delay  延迟时间
     * @param unit   时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> schedule(TaskModule module, Runnable task, long delay, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        return scheduler.schedule(() -> keyedVirtualExecutor.execute(module, task), delay, unit);
    }

    /**
     * 以固定速率周期执行任务（单链：仅模块）
     * 每次到点后自动提交到 {@link KeyedVirtualExecutor} 按模块有序执行
     *
     * @param module       模块枚举
     * @param task         要执行的业务任务
     * @param initialDelay 初始延迟时间
     * @param period       周期间隔
     * @param unit         时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(TaskModule module, Runnable task,
                                                  long initialDelay, long period, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        return scheduler.scheduleAtFixedRate(() -> keyedVirtualExecutor.execute(module, task),
                initialDelay, period, unit);
    }

    /**
     * 以固定延迟周期执行任务（单链：仅模块）
     * 保证上一次任务在虚拟线程中执行完毕后，才开始计时下一次延迟
     *
     * <p>实现方式：使用 {@link KeyedVirtualExecutor#submit} 提交任务获取 {@link CompletableFuture}，
     * 在 {@code whenComplete} 回调中自递归调度下一次，从而精确控制延迟起点。
     * 返回 {@link CancellableScheduledFuture}，调用 {@code cancel()} 可终止整个周期调度链。
     *
     * @param module       模块枚举
     * @param task         要执行的业务任务
     * @param initialDelay 初始延迟时间
     * @param delay        固定延迟时间
     * @param unit         时间单位
     * @return CancellableScheduledFuture，可用于取消整个周期调度
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(TaskModule module, Runnable task,
                                                     long initialDelay, long delay, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        Runnable scheduleOnce = new Runnable() {
            @Override
            public void run() {
                CompletableFuture<Void> future = keyedVirtualExecutor.submit(module, task);
                future.whenComplete((v, ex) -> {
                    if (!cancelled.get() && !isShutdown()) {
                        futureRef.set(scheduler.schedule(this, delay, unit));
                    }
                });
            }
        };

        futureRef.set(scheduler.schedule(scheduleOnce, initialDelay, unit));
        return new CancellableScheduledFuture(futureRef, cancelled);
    }

    // ======================== TaskKey 延迟执行 ========================

    /**
     * 延迟执行一次性任务（使用 TaskKey）
     * 到点后自动提交到 {@link KeyedVirtualExecutor} 按 key 有序执行
     *
     * @param key   任务标识
     * @param task  要执行的业务任务
     * @param delay 延迟时间
     * @param unit  时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> schedule(TaskKey key, Runnable task, long delay, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        return scheduler.schedule(() -> keyedVirtualExecutor.execute(key, task), delay, unit);
    }

    /**
     * 以固定速率周期执行任务（使用 TaskKey）
     * 每次到点后自动提交到 {@link KeyedVirtualExecutor} 按 key 有序执行
     *
     * @param key          任务标识
     * @param task         要执行的业务任务
     * @param initialDelay 初始延迟时间
     * @param period       周期间隔
     * @param unit         时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(TaskKey key, Runnable task,
                                                  long initialDelay, long period, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        return scheduler.scheduleAtFixedRate(() -> keyedVirtualExecutor.execute(key, task),
                initialDelay, period, unit);
    }

    /**
     * 以固定延迟周期执行任务（使用 TaskKey）
     * 保证上一次任务在虚拟线程中执行完毕后，才开始计时下一次延迟
     *
     * <p>实现方式：使用 {@link KeyedVirtualExecutor#submit} 提交任务获取 {@link CompletableFuture}，
     * 在 {@code whenComplete} 回调中自递归调度下一次，从而精确控制延迟起点。
     * 返回 {@link CancellableScheduledFuture}，调用 {@code cancel()} 可终止整个周期调度链。
     *
     * @param key          任务标识
     * @param task         要执行的业务任务
     * @param initialDelay 初始延迟时间
     * @param delay        固定延迟时间
     * @param unit         时间单位
     * @return CancellableScheduledFuture，可用于取消整个周期调度
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(TaskKey key, Runnable task,
                                                     long initialDelay, long delay, TimeUnit unit) {
        if (isShutdown()) {
            return null;
        }
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();

        Runnable scheduleOnce = new Runnable() {
            @Override
            public void run() {
                CompletableFuture<Void> future = keyedVirtualExecutor.submit(key, task);
                future.whenComplete((v, ex) -> {
                    if (!cancelled.get() && !isShutdown()) {
                        futureRef.set(scheduler.schedule(this, delay, unit));
                    }
                });
            }
        };

        futureRef.set(scheduler.schedule(scheduleOnce, initialDelay, unit));
        return new CancellableScheduledFuture(futureRef, cancelled);
    }

    // ======================== 销毁 ========================

    /**
     * 销毁定时调度器
     * 先停止接收新任务，等待已调度任务完成后关闭
     */
    @PreDestroy
    public void destroy() {
        LoggerUtil.debug("GlobalScheduler 开始销毁...");

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    LoggerUtil.warn("定时调度器未能在 30 秒内完成关闭，强制关闭");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                LoggerUtil.error("等待定时调度器关闭时被中断", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        LoggerUtil.debug("GlobalScheduler 已销毁");
    }
}
