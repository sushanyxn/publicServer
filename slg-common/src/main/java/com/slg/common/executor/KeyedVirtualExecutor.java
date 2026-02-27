package com.slg.common.executor;

import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 按 Key 有序的虚拟线程执行器
 * 同一 {@link TaskKey} 的任务串行执行，不同 {@link TaskKey} 的任务并发执行
 *
 * <p>内部使用"两级 Map + 队列 + 消费者"模式：
 * <ul>
 *   <li>第一级：{@link EnumMap}&lt;{@link TaskModule}, {@link ModuleQueue}&gt;，按模块数组下标访问，O(1)</li>
 *   <li>第二级（多链）：{@link ConcurrentHashMap}&lt;Long, {@link KeyedTaskQueue}&gt;，按 id 分链</li>
 *   <li>第二级（单链）：直接持有一个 {@link KeyedTaskQueue}，零查找</li>
 *   <li>仅在没有消费者运行时启动虚拟线程循环 drain 队列</li>
 *   <li>一个虚拟线程可连续执行多个任务，减少线程创建与切换开销</li>
 * </ul>
 *
 * <p>{@link TaskKey} 仅在消费者启动时创建一次（用于线程名和 ThreadLocal），
 * 任务提交和线程判断路径均无 TaskKey 分配，GC 友好。
 *
 * <p>获取方式：
 * <ul>
 *   <li>Spring 注入：{@code @Autowired KeyedVirtualExecutor executor}</li>
 *   <li>静态获取：{@code KeyedVirtualExecutor.getInstance()}</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/07
 */
@Component
public class KeyedVirtualExecutor {

    /**
     * 当前消费者线程正在处理的 TaskKey
     * 用于 {@link #inThread(TaskModule, long)} 判断，避免死锁
     */
    private static final ThreadLocal<TaskKey> CURRENT_KEY = new ThreadLocal<>();

    /**
     * 两级队列结构：模块 → 模块队列容器
     * 第一级使用 EnumMap（数组下标访问），第二级根据多链/单链选择不同存储
     */
    private final EnumMap<TaskModule, ModuleQueue> moduleQueues = new EnumMap<>(TaskModule.class);

    /**
     * 虚拟线程执行器
     */
    private ExecutorService virtualExecutor;

    @Autowired
    private VirtualExecutorHolder virtualExecutorHolder;

    /**
     * 静态实例，供非 Spring 场景使用
     */
    @Getter
    private static KeyedVirtualExecutor instance;

    /**
     * 初始化：构建两级 Map 结构
     */
    @PostConstruct
    public void init() {
        virtualExecutor = virtualExecutorHolder.getExecutor();
        instance = this;

        // 为每个模块预建队列容器
        for (TaskModule module : TaskModule.values()) {
            moduleQueues.put(module, new ModuleQueue(module.isMultiChain()));
        }

        LoggerUtil.debug("KeyedVirtualExecutor 初始化完成（两级 Map 结构）");
    }

    // ======================== 核心执行方法 ========================

    /**
     * 提交任务到指定 key 的队列
     * 同一 key 的任务按提交顺序串行执行，不同 key 的任务并发执行
     *
     * @param key  任务标识
     * @param task 要执行的任务
     */
    public void execute(TaskKey key, Runnable task) {
        if (key == null) {
            LoggerUtil.error("TaskKey 不能为 null");
            return;
        }
        if (key.isSingleChain()) {
            execute(key.module(), task);
        } else {
            execute(key.module(), key.id(), task);
        }
    }

    /**
     * 提交任务到指定模块 + ID 的队列（多链）
     * 同模块下相同 ID 的任务串行执行，不同 ID 的任务并发执行
     * <p>热路径零 TaskKey 分配：直接通过 EnumMap + Long-keyed map 查找队列
     * <p>任务入队前用 {@link SafeRunnable} 包装，保证异常不会导致消费者线程终止
     *
     * @param module 模块枚举
     * @param id     标识（如 playerId、entityId）
     * @param task   要执行的任务
     */
    public void execute(TaskModule module, long id, Runnable task) {
        if (task == null) {
            LoggerUtil.error("任务不能为 null, module={}, id={}", module, id);
            return;
        }

        ModuleQueue mq = moduleQueues.get(module);
        KeyedTaskQueue kq = mq.multiChainQueues.computeIfAbsent(id, k -> new KeyedTaskQueue());
        kq.queue.offer(SafeRunnable.wrap(task));

        // 如果没有消费者在运行，启动一个虚拟线程来消费队列
        if (kq.running.compareAndSet(false, true)) {
            // TaskKey 仅在消费者启动时创建一次，用于线程名和 ThreadLocal
            virtualExecutor.execute(() -> drain(module.toKey(id), kq));
        }
    }

    /**
     * 提交任务到指定模块的队列（单链）
     * 该模块所有任务共用一条串行链
     * <p>热路径零对象分配：直接引用单链队列，TaskKey 使用枚举缓存实例
     * <p>任务入队前用 {@link SafeRunnable} 包装，保证异常不会导致消费者线程终止
     *
     * @param module 模块枚举
     * @param task   要执行的任务
     */
    public void execute(TaskModule module, Runnable task) {
        if (task == null) {
            LoggerUtil.error("任务不能为 null, module={}", module);
            return;
        }

        ModuleQueue mq = moduleQueues.get(module);
        KeyedTaskQueue kq = mq.singleChainQueue;
        kq.queue.offer(SafeRunnable.wrap(task));

        if (kq.running.compareAndSet(false, true)) {
            // 使用 TaskModule 预缓存的单链 TaskKey，零分配
            virtualExecutor.execute(() -> drain(module.toKey(), kq));
        }
    }

    // ======================== 带返回值的提交方法 ========================

    /**
     * 提交带返回值的任务到指定 key 的队列
     * 使用 {@link SafeCallbackRunnable} 包装任务，自动将结果或异常设置到 CompletableFuture
     *
     * @param key  任务标识
     * @param task 要执行的 Callable 任务
     * @param <T>  返回值类型
     * @return CompletableFuture，可获取任务执行结果
     */
    public <T> CompletableFuture<T> submit(TaskKey key, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (key == null) {
            LoggerUtil.error("TaskKey 不能为 null");
            future.completeExceptionally(new NullPointerException("TaskKey 不能为 null"));
            return future;
        }
        if (key.isSingleChain()) {
            return submit(key.module(), task);
        } else {
            return submit(key.module(), key.id(), task);
        }
    }

    /**
     * 提交带返回值的任务到指定 key 的队列（Runnable 版本，无返回值）
     * 使用 {@link SafeCallbackRunnable} 包装任务，自动将完成状态或异常设置到 CompletableFuture
     *
     * @param key  任务标识
     * @param task 要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成
     */
    public CompletableFuture<Void> submit(TaskKey key, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (key == null) {
            LoggerUtil.error("TaskKey 不能为 null");
            future.completeExceptionally(new NullPointerException("TaskKey 不能为 null"));
            return future;
        }
        if (key.isSingleChain()) {
            return submit(key.module(), task);
        } else {
            return submit(key.module(), key.id(), task);
        }
    }

    /**
     * 提交带返回值的 Callable 任务到指定模块 + ID 的队列（多链）
     * 使用 {@link SafeCallbackRunnable} 包装任务，自动将结果或异常设置到 CompletableFuture
     *
     * @param module 模块枚举
     * @param id     标识（如 playerId、entityId）
     * @param task   要执行的 Callable 任务
     * @param <T>    返回值类型
     * @return CompletableFuture，可获取任务执行结果
     */
    public <T> CompletableFuture<T> submit(TaskModule module, long id, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (task == null) {
            LoggerUtil.error("任务不能为 null, module={}, id={}", module, id);
            future.completeExceptionally(new NullPointerException("任务不能为 null"));
            return future;
        }

        ModuleQueue mq = moduleQueues.get(module);
        KeyedTaskQueue kq = mq.multiChainQueues.computeIfAbsent(id, k -> new KeyedTaskQueue());
        kq.queue.offer(SafeCallbackRunnable.wrap(task, future));

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(id), kq));
        }
        return future;
    }

    /**
     * 提交 Runnable 任务到指定模块 + ID 的队列（多链），返回 CompletableFuture
     * 使用 {@link SafeCallbackRunnable} 包装任务，自动将完成状态或异常设置到 CompletableFuture
     *
     * @param module 模块枚举
     * @param id     标识（如 playerId、entityId）
     * @param task   要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成
     */
    public CompletableFuture<Void> submit(TaskModule module, long id, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (task == null) {
            LoggerUtil.error("任务不能为 null, module={}, id={}", module, id);
            future.completeExceptionally(new NullPointerException("任务不能为 null"));
            return future;
        }

        ModuleQueue mq = moduleQueues.get(module);
        KeyedTaskQueue kq = mq.multiChainQueues.computeIfAbsent(id, k -> new KeyedTaskQueue());
        kq.queue.offer(SafeCallbackRunnable.wrap(task, future));

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(id), kq));
        }
        return future;
    }

    /**
     * 提交带返回值的 Callable 任务到指定模块的队列（单链）
     * 使用 {@link SafeCallbackRunnable} 包装任务，自动将结果或异常设置到 CompletableFuture
     *
     * @param module 模块枚举
     * @param task   要执行的 Callable 任务
     * @param <T>    返回值类型
     * @return CompletableFuture，可获取任务执行结果
     */
    public <T> CompletableFuture<T> submit(TaskModule module, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (task == null) {
            LoggerUtil.error("任务不能为 null, module={}", module);
            future.completeExceptionally(new NullPointerException("任务不能为 null"));
            return future;
        }

        ModuleQueue mq = moduleQueues.get(module);
        KeyedTaskQueue kq = mq.singleChainQueue;
        kq.queue.offer(SafeCallbackRunnable.wrap(task, future));

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(), kq));
        }
        return future;
    }

    /**
     * 提交 Runnable 任务到指定模块的队列（单链），返回 CompletableFuture
     * 使用 {@link SafeCallbackRunnable} 包装任务，自动将完成状态或异常设置到 CompletableFuture
     *
     * @param module 模块枚举
     * @param task   要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成
     */
    public CompletableFuture<Void> submit(TaskModule module, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (task == null) {
            LoggerUtil.error("任务不能为 null, module={}", module);
            future.completeExceptionally(new NullPointerException("任务不能为 null"));
            return future;
        }

        ModuleQueue mq = moduleQueues.get(module);
        KeyedTaskQueue kq = mq.singleChainQueue;
        kq.queue.offer(SafeCallbackRunnable.wrap(task, future));

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(), kq));
        }
        return future;
    }

    // ======================== 线程判断 ========================

    /**
     * 判断当前线程是否是目标 key 的消费者线程
     * 通过 ThreadLocal 实现，消费者虚拟线程在 drain 开始时设置、结束时清除
     *
     * @param key 任务标识
     * @return true 表示当前就是该 key 的消费者线程
     */
    public boolean inThread(TaskKey key) {
        if (key == null) {
            return false;
        }
        TaskKey current = CURRENT_KEY.get();
        return current != null && current.module() == key.module() && current.id() == key.id();
    }

    /**
     * 判断当前线程是否是目标模块 + ID 的消费者线程（多链）
     * <p>零对象分配：直接比较枚举引用和 long 原始值，不创建 TaskKey
     *
     * @param module 模块枚举
     * @param id     标识
     * @return true 表示当前就是该 key 的消费者线程
     */
    public boolean inThread(TaskModule module, long id) {
        TaskKey current = CURRENT_KEY.get();
        return current != null && current.module() == module && current.id() == id;
    }

    /**
     * 判断当前线程是否是目标模块的消费者线程（单链）
     * <p>零对象分配：直接比较枚举引用和固定 id=0，不创建 TaskKey
     *
     * @param module 模块枚举
     * @return true 表示当前就是该模块的消费者线程
     */
    public boolean inThread(TaskModule module) {
        TaskKey current = CURRENT_KEY.get();
        return current != null && current.module() == module && current.id() == 0L;
    }

    /**
     * 获取当前消费者线程正在处理的 TaskKey
     *
     * @return 当前 TaskKey，非消费者线程返回 null
     */
    public static TaskKey currentTaskKey() {
        return CURRENT_KEY.get();
    }

    // ======================== 消费者核心逻辑 ========================

    /**
     * 消费者循环：drain 指定 key 的任务队列
     * 设置线程名和 ThreadLocal 后循环执行队列中的所有任务
     *
     * <p>任务已由 {@link SafeRunnable} 包装，异常在 SafeRunnable 内部被捕获并记录日志，
     * 不会传播到 drain 层。外层 catch 仅作为兜底安全网。
     *
     * @param key 任务标识（仅在消费者启动时创建一次）
     * @param kq  该 key 对应的队列
     */
    private void drain(TaskKey key, KeyedTaskQueue kq) {
        // 设置消费者线程的上下文：线程名 + ThreadLocal
        Thread.currentThread().setName("vt-" + key);
        CURRENT_KEY.set(key);

        try {
            Runnable task;
            while ((task = kq.queue.poll()) != null) {
                // 任务已被 SafeRunnable 包装，异常不会外泄
                task.run();
            }
        } catch (Throwable e) {
            // 兜底安全网：正常情况下 SafeRunnable 已处理所有异常，此处不应触发
            LoggerUtil.error("drain 异常（SafeRunnable 未捕获的意外错误）, key={}", key, e);
        } finally {
            // 清除上下文
            CURRENT_KEY.remove();

            // 标记消费者结束
            kq.running.set(false);

            // 防竞态：set(false) 之后可能有新任务入队但没有启动消费者
            // 如果队列不为空且成功 CAS，继续 drain
            if (!kq.queue.isEmpty() && kq.running.compareAndSet(false, true)) {
                virtualExecutor.execute(() -> drain(key, kq));
            } else if (kq.queue.isEmpty() && !kq.running.get()) {
                // 队列为空且无消费者运行，清理该 key 防止内存泄漏（仅多链模块需要清理）
                if (!key.isSingleChain()) {
                    ModuleQueue mq = moduleQueues.get(key.module());
                    if (mq.multiChainQueues != null) {
                        mq.multiChainQueues.remove(key.id(), kq);
                    }
                }
            }
        }
    }

    // ======================== 空闲队列清理 ========================

    /**
     * 清理空闲队列（兜底机制）
     * 遍历所有多链模块，移除队列为空且无消费者运行的 key，防止长期运行积累大量空队列
     *
     * <p>此方法由 {@link GlobalScheduler} 定期调用（如每 5 分钟），
     * 作为 drain() 结束时清理的补充兜底机制。
     * 单链模块的队列是固定引用，不参与清理。
     */
    public void cleanupIdleQueues() {
        int totalRemoved = 0;

        for (TaskModule module : TaskModule.values()) {
            if (!module.isMultiChain()) {
                // 单链模块不需要清理
                continue;
            }

            ModuleQueue mq = moduleQueues.get(module);
            if (mq.multiChainQueues == null) {
                continue;
            }

            int before = mq.multiChainQueues.size();
            mq.multiChainQueues.entrySet().removeIf(e -> {
                KeyedTaskQueue kq = e.getValue();
                return kq.queue.isEmpty() && !kq.running.get();
            });
            totalRemoved += before - mq.multiChainQueues.size();
        }

        if (totalRemoved > 0) {
            LoggerUtil.debug("清理空闲队列: 移除={}", totalRemoved);
        }
    }

    // ======================== 关闭与等待 ========================

    /**
     * 关闭等待超时时间（毫秒）
     * 超时后不再等待，输出诊断信息并返回
     */
    private static final long SHUTDOWN_TIMEOUT_MS = 30_000;

    /**
     * 等待所有队列中的任务执行完毕（带超时）
     * 阻塞当前线程，直到所有消费者完成 drain 且没有待处理任务，或超时退出
     *
     * <p>典型使用场景：服务器关闭时，确保所有任务执行完毕后再销毁线程池
     *
     * @return true 表示所有任务已完成，false 表示超时退出（仍有未完成任务）
     */
    public boolean awaitAllTasksComplete() {
        LoggerUtil.debug("开始等待所有 KeyedVirtualExecutor 任务完成（超时={}ms）...", SHUTDOWN_TIMEOUT_MS);

        long startTime = System.currentTimeMillis();
        int checkCount = 0;

        while (true) {
            checkCount++;

            if (isAllIdle()) {
                long duration = System.currentTimeMillis() - startTime;
                LoggerUtil.debug("所有 KeyedVirtualExecutor 任务已完成！等待时长={}ms", duration);
                return true;
            }

            // 检查超时
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= SHUTDOWN_TIMEOUT_MS) {
                LoggerUtil.warn("等待任务完成超时（已等待{}ms），以下队列仍有未完成任务：", elapsed);
                logActiveQueues();
                return false;
            }

            // 每 3 秒输出一次诊断信息
            if (checkCount % 30 == 0) {
                LoggerUtil.debug("等待中... 已等待={}秒，活跃队列详情：", elapsed / 1000);
                logActiveQueues();
            }

            // 休眠 100ms 后再检查
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LoggerUtil.warn("等待 KeyedVirtualExecutor 任务完成时被中断");
                return false;
            }
        }
    }

    /**
     * 检查所有模块的队列是否全部空闲
     *
     * @return true 表示所有队列为空且无消费者运行
     */
    private boolean isAllIdle() {
        for (TaskModule module : TaskModule.values()) {
            ModuleQueue mq = moduleQueues.get(module);
            if (mq == null) {
                continue;
            }

            if (module.isMultiChain()) {
                // 多链：检查每个 id 的队列
                if (mq.multiChainQueues != null) {
                    for (KeyedTaskQueue kq : mq.multiChainQueues.values()) {
                        if (!kq.queue.isEmpty() || kq.running.get()) {
                            return false;
                        }
                    }
                }
            } else {
                // 单链：检查唯一的队列
                if (mq.singleChainQueue != null) {
                    if (!mq.singleChainQueue.queue.isEmpty() || mq.singleChainQueue.running.get()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 输出当前仍然活跃（非空闲）的队列诊断信息
     * 用于关闭等待期间定位哪些模块/key 的任务还未完成
     */
    private void logActiveQueues() {
        for (TaskModule module : TaskModule.values()) {
            ModuleQueue mq = moduleQueues.get(module);
            if (mq == null) {
                continue;
            }

            if (module.isMultiChain()) {
                if (mq.multiChainQueues != null) {
                    for (Map.Entry<Long, KeyedTaskQueue> entry : mq.multiChainQueues.entrySet()) {
                        KeyedTaskQueue kq = entry.getValue();
                        if (!kq.queue.isEmpty() || kq.running.get()) {
                            LoggerUtil.warn("  活跃队列: module={}, id={}, queueSize={}, running={}",
                                    module, entry.getKey(), kq.queue.size(), kq.running.get());
                        }
                    }
                }
            } else {
                if (mq.singleChainQueue != null) {
                    KeyedTaskQueue kq = mq.singleChainQueue;
                    if (!kq.queue.isEmpty() || kq.running.get()) {
                        LoggerUtil.warn("  活跃队列: module={} (单链), queueSize={}, running={}",
                                module, kq.queue.size(), kq.running.get());
                    }
                }
            }
        }
    }

    /**
     * 销毁执行器
     * <p>销毁流程：
     * <ol>
     *   <li>等待所有任务完成（带超时）</li>
     *   <li>如果超时仍有任务未完成，强制关闭虚拟线程池以中断阻塞的任务</li>
     *   <li>清理队列数据结构</li>
     * </ol>
     *
     * <p>注意：由于 {@link VirtualExecutorHolder} 依赖顺序在本类之后销毁，
     * 如果本方法不主动关闭虚拟线程池，阻塞的虚拟线程将永远得不到中断，
     * 导致 awaitAllTasksComplete 无限等待。因此超时后由本方法统一强制关闭。
     */
    @PreDestroy
    public void destroy() {
        LoggerUtil.debug("KeyedVirtualExecutor 开始销毁...");

        boolean allCompleted = awaitAllTasksComplete();

        if (!allCompleted) {
            // 超时仍有任务未完成，强制关闭虚拟线程池以中断阻塞的虚拟线程
            LoggerUtil.warn("关闭等待超时，强制关闭虚拟线程池以中断阻塞任务...");
            virtualExecutor.shutdownNow();

            // 给被中断的任务一点时间响应中断
            try {
                if (!virtualExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LoggerUtil.warn("虚拟线程池强制关闭后仍有线程未终止，放弃等待");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LoggerUtil.warn("等待虚拟线程池强制关闭时被中断");
            }
        }

        moduleQueues.clear();
        LoggerUtil.debug("KeyedVirtualExecutor 已销毁");
    }

    // ======================== 内部数据结构 ========================

    /**
     * 每个模块的队列容器
     * 根据模块的多链/单链属性，选择不同的存储方式
     */
    private static class ModuleQueue {

        /**
         * 单链队列（单链模块使用，多链模块为 null）
         * 该模块所有任务共用此队列，直接引用，零查找
         */
        final KeyedTaskQueue singleChainQueue;

        /**
         * 多链队列映射（多链模块使用，单链模块为 null）
         * 按 long id 索引，同 id 串行，不同 id 并发
         */
        final ConcurrentHashMap<Long, KeyedTaskQueue> multiChainQueues;

        ModuleQueue(boolean multiChain) {
            if (multiChain) {
                this.singleChainQueue = null;
                this.multiChainQueues = new ConcurrentHashMap<>();
            } else {
                this.singleChainQueue = new KeyedTaskQueue();
                this.multiChainQueues = null;
            }
        }
    }

    /**
     * 每个 key 对应的任务队列和消费者运行状态
     */
    private static class KeyedTaskQueue {

        /**
         * 任务队列（无锁 CAS 入队/出队）
         */
        final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

        /**
         * 是否有消费者虚拟线程正在 drain 此队列
         * true 表示有消费者在运行，false 表示空闲
         */
        final AtomicBoolean running = new AtomicBoolean(false);
    }
}
