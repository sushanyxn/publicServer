package com.slg.common.executor.core;

import com.slg.common.executor.TaskModule;
import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.slg.common.executor.core.ExecutorConstants.*;

/**
 * 按 Key 有序的虚拟线程执行器
 * 同一 {@link TaskKey} 的任务串行执行，不同 {@link TaskKey} 的任务并发执行
 *
 * <p>内部使用"两级 Map + 队列 + 消费者"模式：
 * <ul>
 *   <li>第一级：{@link EnumMap}&lt;{@link TaskModule}, {@link ModuleQueue}&gt;，按模块数组下标访问，O(1)</li>
 *   <li>第二级（多链）：{@link ConcurrentHashMap}&lt;Long, {@link KeyedTaskQueue}&gt;，按 id 分链；
 *       使用 {@link ConcurrentHashMap#compute} 将"获取/创建队列"与"入队"合并为原子操作，
 *       与 {@link #cleanupIdleQueues()} 的 {@link ConcurrentHashMap#computeIfPresent} 在 bucket 锁层面互斥</li>
 *   <li>第二级（单链）：直接持有一个 {@link KeyedTaskQueue}，零查找；
 *       采用先递增计数器再检查容量的方式，确保多线程并发投递时容量限制精确</li>
 *   <li>仅在没有消费者运行时启动虚拟线程循环 drain 队列</li>
 *   <li>一个虚拟线程可连续执行多个任务，减少线程创建与切换开销</li>
 * </ul>
 *
 * <p>容灾机制：
 * <ul>
 *   <li>队列容量限制：每条链有容量上限（由 {@link TaskModule#getMaxQueueSize()} 控制），
 *       超过后拒绝新任务并记录日志</li>
 *   <li>Watchdog：定期扫描所有活跃消费者，检测执行超时的任务，
 *       超过告警阈值记录线程栈，超过中断阈值强制中断</li>
 *   <li>关闭超时：{@link #awaitAllTasksComplete(long)} 带超时保护，避免关闭流程永久阻塞</li>
 * </ul>
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
     * <p>使用 {@link ConcurrentHashMap#compute} 将"获取/创建队列"与"入队"合并为原子操作，
     * 与 {@link #cleanupIdleQueues()} 的 {@link ConcurrentHashMap#computeIfPresent} 在 bucket 锁层面互斥，
     * 消除竞态窗口，保证串行语义不被破坏
     * <p>任务入队前用 {@link SafeRunnable} 包装，保证异常不会导致消费者线程终止
     * <p>队列满时拒绝任务并记录 ERROR 日志
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
        Runnable wrapped = SafeRunnable.wrap(task);
        boolean[] offered = {false};

        KeyedTaskQueue kq = mq.multiChainQueues.compute(id, (k, existing) -> {
            if (existing == null) {
                existing = new KeyedTaskQueue();
            }
            int maxSize = module.getMaxQueueSize();
            if (maxSize > 0 && existing.size.get() >= maxSize) {
                existing.rejectedCount.incrementAndGet();
                return existing;
            }
            existing.queue.offer(wrapped);
            existing.size.incrementAndGet();
            offered[0] = true;
            return existing;
        });

        if (!offered[0]) {
            LoggerUtil.error("任务被拒绝(队列已满): module={}, id={}, maxSize={}",
                    module, id, module.getMaxQueueSize());
            return;
        }

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(id), kq));
        }
    }

    /**
     * 提交任务到指定模块的队列（单链）
     * 该模块所有任务共用一条串行链
     * <p>热路径零对象分配：直接引用单链队列，TaskKey 使用枚举缓存实例
     * <p>任务入队前用 {@link SafeRunnable} 包装，保证异常不会导致消费者线程终止
     * <p>采用先递增计数器再检查容量的方式，确保多线程并发投递时容量限制精确生效
     * <p>队列满时拒绝任务并记录 ERROR 日志
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

        int maxSize = module.getMaxQueueSize();
        int newSize = kq.size.incrementAndGet();
        if (maxSize > 0 && newSize > maxSize) {
            kq.size.decrementAndGet();
            kq.rejectedCount.incrementAndGet();
            LoggerUtil.error("任务被拒绝(队列已满): module={}, queueSize={}, maxSize={}",
                    module, newSize - 1, maxSize);
            return;
        }

        kq.queue.offer(SafeRunnable.wrap(task));

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(), kq));
        }
    }

    // ======================== 带返回值的提交方法 ========================

    /**
     * 提交带返回值的任务到指定 key 的队列
     *
     * @param key  任务标识
     * @param task 要执行的 Callable 任务
     * @param <T>  返回值类型
     * @return CompletableFuture，可获取任务执行结果；队列满时返回异常完成的 Future
     */
    public <T> CompletableFuture<T> submit(TaskKey key, Callable<T> task) {
        if (key == null) {
            return CompletableFuture.failedFuture(new NullPointerException("TaskKey 不能为 null"));
        }
        if (key.isSingleChain()) {
            return submit(key.module(), task);
        } else {
            return submit(key.module(), key.id(), task);
        }
    }

    /**
     * 提交带返回值的任务到指定 key 的队列（Runnable 版本，无返回值）
     *
     * @param key  任务标识
     * @param task 要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成；队列满时返回异常完成的 Future
     */
    public CompletableFuture<Void> submit(TaskKey key, Runnable task) {
        if (key == null) {
            return CompletableFuture.failedFuture(new NullPointerException("TaskKey 不能为 null"));
        }
        if (key.isSingleChain()) {
            return submit(key.module(), task);
        } else {
            return submit(key.module(), key.id(), task);
        }
    }

    /**
     * 提交带返回值的 Callable 任务到指定模块 + ID 的队列（多链）
     * <p>使用 {@link ConcurrentHashMap#compute} 原子化入队，与 {@link #cleanupIdleQueues()} 互斥
     *
     * @param module 模块枚举
     * @param id     标识（如 playerId、entityId）
     * @param task   要执行的 Callable 任务
     * @param <T>    返回值类型
     * @return CompletableFuture，可获取任务执行结果；队列满时返回异常完成的 Future
     */
    public <T> CompletableFuture<T> submit(TaskModule module, long id, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (task == null) {
            future.completeExceptionally(new NullPointerException("任务不能为 null"));
            return future;
        }

        ModuleQueue mq = moduleQueues.get(module);
        boolean[] offered = {false};

        KeyedTaskQueue kq = mq.multiChainQueues.compute(id, (k, existing) -> {
            if (existing == null) {
                existing = new KeyedTaskQueue();
            }
            int maxSize = module.getMaxQueueSize();
            if (maxSize > 0 && existing.size.get() >= maxSize) {
                existing.rejectedCount.incrementAndGet();
                return existing;
            }
            existing.queue.offer(SafeCallbackRunnable.wrap(task, future));
            existing.size.incrementAndGet();
            offered[0] = true;
            return existing;
        });

        if (!offered[0]) {
            future.completeExceptionally(rejectedException(module, id));
            return future;
        }

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(id), kq));
        }
        return future;
    }

    /**
     * 提交 Runnable 任务到指定模块 + ID 的队列（多链），返回 CompletableFuture
     * <p>使用 {@link ConcurrentHashMap#compute} 原子化入队，与 {@link #cleanupIdleQueues()} 互斥
     *
     * @param module 模块枚举
     * @param id     标识（如 playerId、entityId）
     * @param task   要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成；队列满时返回异常完成的 Future
     */
    public CompletableFuture<Void> submit(TaskModule module, long id, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (task == null) {
            future.completeExceptionally(new NullPointerException("任务不能为 null"));
            return future;
        }

        ModuleQueue mq = moduleQueues.get(module);
        boolean[] offered = {false};

        KeyedTaskQueue kq = mq.multiChainQueues.compute(id, (k, existing) -> {
            if (existing == null) {
                existing = new KeyedTaskQueue();
            }
            int maxSize = module.getMaxQueueSize();
            if (maxSize > 0 && existing.size.get() >= maxSize) {
                existing.rejectedCount.incrementAndGet();
                return existing;
            }
            existing.queue.offer(SafeCallbackRunnable.wrap(task, future));
            existing.size.incrementAndGet();
            offered[0] = true;
            return existing;
        });

        if (!offered[0]) {
            future.completeExceptionally(rejectedException(module, id));
            return future;
        }

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(id), kq));
        }
        return future;
    }

    /**
     * 提交带返回值的 Callable 任务到指定模块的队列（单链）
     * <p>采用先递增计数器再检查容量的方式，确保多线程并发投递时容量限制精确生效
     *
     * @param module 模块枚举
     * @param task   要执行的 Callable 任务
     * @param <T>    返回值类型
     * @return CompletableFuture，可获取任务执行结果；队列满时返回异常完成的 Future
     */
    public <T> CompletableFuture<T> submit(TaskModule module, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (task == null) {
            future.completeExceptionally(new NullPointerException("任务不能为 null"));
            return future;
        }

        ModuleQueue mq = moduleQueues.get(module);
        KeyedTaskQueue kq = mq.singleChainQueue;

        int maxSize = module.getMaxQueueSize();
        int newSize = kq.size.incrementAndGet();
        if (maxSize > 0 && newSize > maxSize) {
            kq.size.decrementAndGet();
            kq.rejectedCount.incrementAndGet();
            future.completeExceptionally(rejectedException(module, SINGLE_CHAIN_ID));
            return future;
        }

        kq.queue.offer(SafeCallbackRunnable.wrap(task, future));

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(), kq));
        }
        return future;
    }

    /**
     * 提交 Runnable 任务到指定模块的队列（单链），返回 CompletableFuture
     * <p>采用先递增计数器再检查容量的方式，确保多线程并发投递时容量限制精确生效
     *
     * @param module 模块枚举
     * @param task   要执行的 Runnable 任务
     * @return CompletableFuture&lt;Void&gt;，可用于等待任务完成；队列满时返回异常完成的 Future
     */
    public CompletableFuture<Void> submit(TaskModule module, Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (task == null) {
            future.completeExceptionally(new NullPointerException("任务不能为 null"));
            return future;
        }

        ModuleQueue mq = moduleQueues.get(module);
        KeyedTaskQueue kq = mq.singleChainQueue;

        int maxSize = module.getMaxQueueSize();
        int newSize = kq.size.incrementAndGet();
        if (maxSize > 0 && newSize > maxSize) {
            kq.size.decrementAndGet();
            kq.rejectedCount.incrementAndGet();
            future.completeExceptionally(rejectedException(module, SINGLE_CHAIN_ID));
            return future;
        }

        kq.queue.offer(SafeCallbackRunnable.wrap(task, future));

        if (kq.running.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(module.toKey(), kq));
        }
        return future;
    }

    // ======================== 容量检查 ========================

    private static RejectedExecutionException rejectedException(TaskModule module, long id) {
        return new RejectedExecutionException("队列已满: module=" + module + ", id=" + id);
    }

    // ======================== 队列大小查询 ========================

    /**
     * 获取指定模块 + ID 的队列大小（多链）
     *
     * @param module 模块枚举
     * @param id     链标识
     * @return 队列中的任务数量
     */
    public int getQueueSize(TaskModule module, long id) {
        ModuleQueue mq = moduleQueues.get(module);
        if (mq == null) {
            return 0;
        }
        if (module.isMultiChain()) {
            KeyedTaskQueue kq = mq.multiChainQueues.get(id);
            return kq != null ? kq.size.get() : 0;
        } else {
            return mq.singleChainQueue != null ? mq.singleChainQueue.size.get() : 0;
        }
    }

    /**
     * 获取指定模块的队列大小（单链）
     *
     * @param module 模块枚举
     * @return 队列中的任务数量
     */
    public int getQueueSize(TaskModule module) {
        return getQueueSize(module, SINGLE_CHAIN_ID);
    }

    // ======================== 指标 ========================

    /**
     * 获取执行器运行时指标快照
     * 遍历所有模块和链，收集队列大小、拒绝数、活跃消费者数等信息
     * <p>此方法是线程安全的，返回的是近似快照数据
     *
     * @return 指标快照
     */
    public ExecutorMetrics getMetrics() {
        Map<TaskModule, ExecutorMetrics.ModuleMetrics> modules = new EnumMap<>(TaskModule.class);

        for (TaskModule module : TaskModule.values()) {
            ModuleQueue mq = moduleQueues.get(module);
            if (mq == null) {
                continue;
            }

            int totalQueueSize = 0;
            long totalRejected = 0;
            int activeConsumers = 0;
            int chainCount = 0;

            if (module.isMultiChain()) {
                if (mq.multiChainQueues != null) {
                    for (KeyedTaskQueue kq : mq.multiChainQueues.values()) {
                        totalQueueSize += kq.size.get();
                        totalRejected += kq.rejectedCount.get();
                        if (kq.running.get()) {
                            activeConsumers++;
                        }
                        chainCount++;
                    }
                }
            } else {
                if (mq.singleChainQueue != null) {
                    totalQueueSize = mq.singleChainQueue.size.get();
                    totalRejected = mq.singleChainQueue.rejectedCount.get();
                    activeConsumers = mq.singleChainQueue.running.get() ? 1 : 0;
                    chainCount = 1;
                }
            }

            modules.put(module, new ExecutorMetrics.ModuleMetrics(
                    module.isMultiChain(), totalQueueSize, totalRejected, activeConsumers, chainCount));
        }

        return new ExecutorMetrics(modules);
    }

    // ======================== 线程判断 ========================

    /**
     * 判断当前线程是否是目标 key 的消费者线程
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
        return current != null && current.module() == module && current.id() == SINGLE_CHAIN_ID;
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
     * <p>每个任务执行前记录开始时间和当前线程引用，供 Watchdog 检测超时任务。
     *
     * @param key 任务标识（仅在消费者启动时创建一次）
     * @param kq  该 key 对应的队列
     */
    private void drain(TaskKey key, KeyedTaskQueue kq) {
        Thread.currentThread().setName("vt-" + key);
        CURRENT_KEY.set(key);
        kq.drainThread = Thread.currentThread();

        try {
            Runnable task;
            while ((task = kq.queue.poll()) != null) {
                kq.size.decrementAndGet();
                kq.taskStartNanos = System.nanoTime();
                task.run();
                kq.taskStartNanos = 0;
            }
        } catch (Throwable e) {
            LoggerUtil.error("drain 异常（SafeRunnable 未捕获的意外错误）, key={}", key, e);
        } finally {
            kq.drainThread = null;
            kq.taskStartNanos = 0;
            CURRENT_KEY.remove();

            kq.running.set(false);

            // 防竞态：set(false) 之后可能有新任务入队但没有启动消费者
            if (!kq.queue.isEmpty() && kq.running.compareAndSet(false, true)) {
                virtualExecutor.execute(() -> drain(key, kq));
            }
            // 不再主动 remove 队列，由 cleanupIdleQueues 统一清理，避免竞态条件破坏串行保证
        }
    }

    // ======================== Watchdog ========================

    /**
     * Watchdog 扫描：检测执行超时的任务
     * <p>遍历所有活跃消费者，根据 {@link KeyedTaskQueue#taskStartNanos} 判断任务执行时长：
     * <ul>
     *   <li>超过告警阈值：记录 WARN 日志并输出线程栈</li>
     *   <li>超过中断阈值：中断消费者虚拟线程，使其有机会从阻塞中恢复</li>
     * </ul>
     * <p>此方法由 {@link GlobalScheduler} 定期调用，在平台调度线程中执行，不阻塞业务。
     */
    public void runWatchdog() {
        for (TaskModule module : TaskModule.values()) {
            ModuleQueue mq = moduleQueues.get(module);
            if (mq == null) {
                continue;
            }

            if (module.isMultiChain()) {
                if (mq.multiChainQueues != null) {
                    for (Map.Entry<Long, KeyedTaskQueue> entry : mq.multiChainQueues.entrySet()) {
                        checkTaskTimeout(module, entry.getKey(), entry.getValue());
                    }
                }
            } else {
                if (mq.singleChainQueue != null) {
                    checkTaskTimeout(module, SINGLE_CHAIN_ID, mq.singleChainQueue);
                }
            }
        }
    }

    /**
     * 检查单个队列的当前任务是否超时
     */
    private void checkTaskTimeout(TaskModule module, long id, KeyedTaskQueue kq) {
        long startNanos = kq.taskStartNanos;
        if (startNanos == 0) {
            return;
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        Thread thread = kq.drainThread;

        if (elapsedMs >= WATCHDOG_INTERRUPT_THRESHOLD_MS && thread != null) {
            LoggerUtil.error("[Watchdog] 任务执行超时({}ms)，强制中断: module={}, id={}, thread={}",
                    elapsedMs, module, id, thread.getName());
            thread.interrupt();
        } else if (elapsedMs >= WATCHDOG_WARN_THRESHOLD_MS && thread != null) {
            String stackTrace = Arrays.stream(thread.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n    at "));
            LoggerUtil.warn("[Watchdog] 任务执行过慢({}ms): module={}, id={}, queueSize={}\n    at {}",
                    elapsedMs, module, id, kq.size.get(), stackTrace);
        }
    }

    // ======================== 空闲队列清理 ========================

    /**
     * 清理空闲队列（兜底机制）
     * 遍历所有多链模块，使用 {@link ConcurrentHashMap#computeIfPresent} 原子化检查并移除
     * 队列为空且无消费者运行的 key，防止长期运行积累大量空队列
     *
     * <p>此方法由 {@link GlobalScheduler} 定期调用（如每 5 分钟），
     * 单链模块的队列是固定引用，不参与清理。
     *
     * <p>使用 computeIfPresent 保证"检查状态 + 移除"的原子性，
     * 避免在检查与移除之间有新任务入队导致任务丢失或串行保证被破坏。
     */
    public void cleanupIdleQueues() {
        int totalRemoved = 0;

        for (TaskModule module : TaskModule.values()) {
            if (!module.isMultiChain()) {
                continue;
            }

            ModuleQueue mq = moduleQueues.get(module);
            if (mq.multiChainQueues == null) {
                continue;
            }

            for (Long id : mq.multiChainQueues.keySet()) {
                KeyedTaskQueue removed = mq.multiChainQueues.computeIfPresent(id, (k, kq) -> {
                    if (kq.queue.isEmpty() && !kq.running.get() && kq.size.get() == 0) {
                        return null;
                    }
                    return kq;
                });
                if (removed == null) {
                    totalRemoved++;
                }
            }
        }

        if (totalRemoved > 0) {
            LoggerUtil.debug("清理空闲队列: 移除={}", totalRemoved);
        }
    }

    // ======================== 关闭与等待 ========================

    /**
     * 等待所有队列中的任务执行完毕（带超时）
     * 阻塞当前线程，直到所有消费者完成 drain 且没有待处理任务，或超时返回。
     *
     * <p>典型使用场景：服务器关闭时，确保所有任务（如持久化）执行完毕后再销毁线程池。
     *
     * @param timeoutMs 超时时间（毫秒），0 或负值表示无超时
     * @return true 表示所有任务已完成，false 表示超时或中断
     */
    public boolean awaitAllTasksComplete(long timeoutMs) {
        LoggerUtil.debug("开始等待所有 KeyedVirtualExecutor 任务完成（超时={}ms）...", timeoutMs);

        long startTime = System.currentTimeMillis();
        long deadline = timeoutMs > 0 ? startTime + timeoutMs : Long.MAX_VALUE;
        int checkCount = 0;

        while (System.currentTimeMillis() < deadline) {
            checkCount++;

            if (isAllIdle()) {
                long duration = System.currentTimeMillis() - startTime;
                LoggerUtil.debug("所有 KeyedVirtualExecutor 任务已完成！等待时长={}ms", duration);
                return true;
            }

            if (checkCount % 30 == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                LoggerUtil.debug("等待中... 已等待={}秒，活跃队列详情：", elapsed / 1000);
                logActiveQueues();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LoggerUtil.warn("等待 KeyedVirtualExecutor 任务完成时被中断");
                return false;
            }
        }

        LoggerUtil.error("等待任务完成超时({}ms)，仍有活跃队列：", timeoutMs);
        logActiveQueues();
        return false;
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
                if (mq.multiChainQueues != null) {
                    for (KeyedTaskQueue kq : mq.multiChainQueues.values()) {
                        if (!kq.queue.isEmpty() || kq.running.get()) {
                            return false;
                        }
                    }
                }
            } else {
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
     * 使用 {@link AtomicInteger} 计数器获取队列大小（O(1)），避免遍历链表
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
                            LoggerUtil.warn("  活跃队列: module={}, id={}, queueSize={}, running={}, rejected={}",
                                    module, entry.getKey(), kq.size.get(), kq.running.get(), kq.rejectedCount.get());
                        }
                    }
                }
            } else {
                if (mq.singleChainQueue != null) {
                    KeyedTaskQueue kq = mq.singleChainQueue;
                    if (!kq.queue.isEmpty() || kq.running.get()) {
                        LoggerUtil.warn("  活跃队列: module={} (单链), queueSize={}, running={}, rejected={}",
                                module, kq.size.get(), kq.running.get(), kq.rejectedCount.get());
                    }
                }
            }
        }
    }

    /**
     * 销毁执行器
     * <p>销毁流程：
     * <ol>
     *   <li>等待所有任务完成（带超时 {@link ExecutorConstants#SHUTDOWN_TIMEOUT_MS}）</li>
     *   <li>如果超时仍有任务未完成，强制关闭虚拟线程池以中断阻塞的任务</li>
     *   <li>清理队列数据结构</li>
     * </ol>
     */
    @PreDestroy
    public void destroy() {
        LoggerUtil.debug("KeyedVirtualExecutor 开始销毁...");

        boolean allCompleted = awaitAllTasksComplete(SHUTDOWN_TIMEOUT_MS);

        if (!allCompleted) {
            LoggerUtil.warn("关闭等待超时，强制关闭虚拟线程池以中断阻塞任务...");
            virtualExecutor.shutdownNow();

            try {
                if (!virtualExecutor.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
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
         */
        final KeyedTaskQueue singleChainQueue;

        /**
         * 多链队列映射（多链模块使用，单链模块为 null）
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
     * 每个 key 对应的任务队列、消费者状态和容灾指标
     */
    private static class KeyedTaskQueue {

        /**
         * 任务队列（无锁 CAS 入队/出队）
         */
        final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

        /**
         * 是否有消费者虚拟线程正在 drain 此队列
         */
        final AtomicBoolean running = new AtomicBoolean(false);

        /**
         * 队列中的任务数量（O(1) 获取，替代 ConcurrentLinkedQueue.size() 的 O(n) 遍历）
         */
        final AtomicInteger size = new AtomicInteger(0);

        /**
         * 累计被拒绝的任务数量
         */
        final AtomicLong rejectedCount = new AtomicLong(0);

        /**
         * 当前正在执行的任务开始时间（纳秒），0 表示无任务在执行
         * Watchdog 通过此字段检测超时任务
         */
        volatile long taskStartNanos;

        /**
         * 当前 drain 消费者线程引用，用于 Watchdog 获取线程栈和中断
         */
        volatile Thread drainThread;
    }
}
