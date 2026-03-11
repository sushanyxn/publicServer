package com.slg.common.executor.core;

import com.slg.common.executor.TaskModule;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 单链模块执行器
 * 封装 {@link KeyedVirtualExecutor} + {@link GlobalScheduler}，提供单链串行的任务执行 API
 *
 * <p>适用于该模块所有任务共用一条串行链的场景（如 SYSTEM、LOGIN、SCENE），
 * 所有提交到该模块的任务按提交顺序串行执行。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 系统任务（单链串行）
 * Executor.System.execute(task);
 * Executor.System.schedule(task, 5, TimeUnit.SECONDS);
 *
 * // 登录任务（单链串行）
 * Executor.Login.execute(task);
 * }</pre>
 *
 * @author yangxunan
 * @date 2026/02/09
 */
public class SingleExecutor {

    /**
     * 所属模块
     */
    private final TaskModule module;

    /**
     * 构造单链模块执行器
     *
     * @param module 任务模块枚举（必须为单链模块）
     */
    public SingleExecutor(TaskModule module) {
        this.module = module;
    }

    // ======================== 任务执行 ========================

    /**
     * 提交任务到该模块的串行链中执行
     * 该模块所有任务共用一条串行链，按提交顺序执行
     *
     * @param task 要执行的任务
     */
    public void execute(Runnable task) {
        KeyedVirtualExecutor.getInstance().execute(module, task);
    }

    // ======================== 带返回值的提交方法 ========================

    /**
     * 提交 Callable 任务到该模块的串行链中执行，返回 CompletableFuture
     * 该模块所有任务共用一条串行链，按提交顺序执行
     *
     * @param task 要执行的 Callable 任务
     * @param <T>  返回值类型
     * @return CompletableFuture，可获取任务执行结果
     */
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return KeyedVirtualExecutor.getInstance().submit(module, task);
    }

    /**
     * 提交 Runnable 任务到该模块的串行链中执行，返回 CompletableFuture
     * 该模块所有任务共用一条串行链，按提交顺序执行
     *
     * @param task 要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成
     */
    public CompletableFuture<Void> submit(Runnable task) {
        return KeyedVirtualExecutor.getInstance().submit(module, task);
    }

    // ======================== 线程判断 ========================

    /**
     * 判断当前线程是否是目标模块的消费者线程
     *
     * @return true 表示当前在目标线程
     */
    public boolean inThread() {
        return KeyedVirtualExecutor.getInstance().inThread(module);
    }

    // ======================== 定时任务 ========================

    /**
     * 延迟执行一次性任务
     *
     * @param task  要执行的任务
     * @param delay 延迟时间
     * @param unit  时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return GlobalScheduler.getInstance().schedule(module, task, delay, unit);
    }

    /**
     * 以固定速率周期执行任务
     *
     * @param task         要执行的任务
     * @param initialDelay 初始延迟时间
     * @param period       周期间隔
     * @param unit         时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return GlobalScheduler.getInstance().scheduleAtFixedRate(module, task, initialDelay, period, unit);
    }

    /**
     * 以固定延迟周期执行任务
     *
     * @param task         要执行的任务
     * @param initialDelay 初始延迟时间
     * @param delay        固定延迟时间
     * @param unit         时间单位
     * @return ScheduledFuture，可用于取消任务
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return GlobalScheduler.getInstance().scheduleWithFixedDelay(module, task, initialDelay, delay, unit);
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
}
