package com.slg.common.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多链模块执行器
 * 封装 {@link KeyedVirtualExecutor} + {@link GlobalScheduler}，提供按 key 分链的任务执行 API
 *
 * <p>适用于需要按 ID 分链串行的模块（如 PLAYER、PERSISTENCE、ROBOT），
 * 同一 key 的任务串行执行，不同 key 的任务可并发执行。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 玩家任务（按 playerId 分链）
 * Executor.Player.execute(playerId, task);
 * Executor.Player.schedule(playerId, task, 5, TimeUnit.SECONDS);
 *
 * // 持久化任务（按实体 ID 分链）
 * Executor.Persistence.execute(entityId, task);
 * }</pre>
 *
 * @author yangxunan
 * @date 2026/02/09
 */
public class MultiExecutor {

    /**
     * 所属模块
     */
    private final TaskModule module;

    /**
     * 构造多链模块执行器
     *
     * @param module 任务模块枚举（必须为多链模块）
     */
    public MultiExecutor(TaskModule module) {
        this.module = module;
    }

    // ======================== 任务执行 ========================

    /**
     * 按 key 分链串行执行任务
     * 同模块下相同 key 的任务串行执行，不同 key 的任务可并发执行
     *
     * @param key  分链标识
     * @param task 要执行的任务
     */
    public void execute(long key, Runnable task) {
        KeyedVirtualExecutor.getInstance().execute(module, key, task);
    }

    /**
     * 按 Object key 分链串行执行任务
     * 内部将 Object 转换为 long 作为分链标识
     *
     * @param key  分链标识（支持 Number、String 等类型）
     * @param task 要执行的任务
     */
    public void execute(Object key, Runnable task) {
        execute(objectKeyToLong(key), task);
    }

    // ======================== 带返回值的提交方法 ========================

    /**
     * 按 key 分链提交 Callable 任务，返回 CompletableFuture
     * 同模块下相同 key 的任务串行执行，不同 key 的任务可并发执行
     *
     * @param key  分链标识
     * @param task 要执行的 Callable 任务
     * @param <T>  返回值类型
     * @return CompletableFuture，可获取任务执行结果
     */
    public <T> CompletableFuture<T> submit(long key, Callable<T> task) {
        return KeyedVirtualExecutor.getInstance().submit(module, key, task);
    }

    /**
     * 按 Object key 分链提交 Callable 任务，返回 CompletableFuture
     * 内部将 Object 转换为 long 作为分链标识
     *
     * @param key  分链标识（支持 Number、String 等类型）
     * @param task 要执行的 Callable 任务
     * @param <T>  返回值类型
     * @return CompletableFuture，可获取任务执行结果
     */
    public <T> CompletableFuture<T> submit(Object key, Callable<T> task) {
        return submit(objectKeyToLong(key), task);
    }

    /**
     * 按 key 分链提交 Runnable 任务，返回 CompletableFuture
     * 同模块下相同 key 的任务串行执行，不同 key 的任务可并发执行
     *
     * @param key  分链标识
     * @param task 要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成
     */
    public CompletableFuture<Void> submit(long key, Runnable task) {
        return KeyedVirtualExecutor.getInstance().submit(module, key, task);
    }

    /**
     * 按 Object key 分链提交 Runnable 任务，返回 CompletableFuture
     * 内部将 Object 转换为 long 作为分链标识
     *
     * @param key  分链标识（支持 Number、String 等类型）
     * @param task 要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成
     */
    public CompletableFuture<Void> submit(Object key, Runnable task) {
        return submit(objectKeyToLong(key), task);
    }

    // ======================== 线程判断 ========================

    /**
     * 判断当前线程是否是目标 key 的消费者线程
     *
     * @param key 分链标识
     * @return true 表示当前在目标线程
     */
    public boolean inThread(long key) {
        return KeyedVirtualExecutor.getInstance().inThread(module, key);
    }

    // ======================== 定时任务 ========================

    /**
     * 延迟执行一次性任务
     *
     * @param key   分链标识
     * @param task  要执行的任务
     * @param delay 延迟时间
     * @param unit  时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> schedule(long key, Runnable task, long delay, TimeUnit unit) {
        return GlobalScheduler.getInstance().schedule(module, key, task, delay, unit);
    }

    /**
     * 以固定速率周期执行任务
     *
     * @param key          分链标识
     * @param task         要执行的任务
     * @param initialDelay 初始延迟时间
     * @param period       周期间隔
     * @param unit         时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(long key, Runnable task, long initialDelay, long period, TimeUnit unit) {
        return GlobalScheduler.getInstance().scheduleAtFixedRate(module, key, task, initialDelay, period, unit);
    }

    /**
     * 以固定延迟周期执行任务
     *
     * @param key          分链标识
     * @param task         要执行的任务
     * @param initialDelay 初始延迟时间
     * @param delay        固定延迟时间
     * @param unit         时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(long key, Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return GlobalScheduler.getInstance().scheduleWithFixedDelay(module, key, task, initialDelay, delay, unit);
    }

    // ======================== 工具方法 ========================

    /**
     * 获取模块枚举
     *
     * @return 模块枚举
     */
    public TaskModule getModule() {
        return module;
    }

    /**
     * 将 Object 类型的 key 转换为 long
     * <ul>
     *   <li>Number 类型（实体 ID 通常为 Long/Integer）：直接取 longValue，保证同 ID 同链</li>
     *   <li>String 类型（如 "insertBatch:xxx"）：hashCode 转 long</li>
     *   <li>其他类型：使用 identityHashCode 兜底</li>
     * </ul>
     *
     * @param key Object 类型的 key
     * @return long 类型的 key
     */
    private static long objectKeyToLong(Object key) {
        if (key instanceof Number num) {
            return num.longValue();
        } else if (key instanceof String str) {
            return str.hashCode();
        } else {
            return System.identityHashCode(key);
        }
    }
}
