package com.slg.common.executor;

import com.slg.common.executor.core.GlobalScheduler;
import com.slg.common.executor.core.KeyedVirtualExecutor;
import com.slg.common.executor.core.VirtualExecutorHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 线程框架性能测试
 * 测量 KeyedVirtualExecutor、GlobalScheduler 在常规任务之外带来的调度与分配开销。
 * 任务体为极轻量 no-op，以便突出框架本身的耗时。
 *
 * @author framework-test
 */
class ExecutorPerformanceTest {

    private static final int WARMUP_COUNT = 2_000;
    private static final int MEASURE_COUNT = 30_000;
    private static final int MULTI_KEY_COUNT = 200;

    private VirtualExecutorHolder virtualExecutorHolder;
    private KeyedVirtualExecutor executor;
    private GlobalScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        virtualExecutorHolder = new VirtualExecutorHolder();
        virtualExecutorHolder.init();

        executor = new KeyedVirtualExecutor();
        Field holderField = KeyedVirtualExecutor.class.getDeclaredField("virtualExecutorHolder");
        holderField.setAccessible(true);
        holderField.set(executor, virtualExecutorHolder);
        executor.init();

        scheduler = new GlobalScheduler();
        Field kveField = GlobalScheduler.class.getDeclaredField("keyedVirtualExecutor");
        kveField.setAccessible(true);
        kveField.set(scheduler, executor);
        scheduler.init();
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            // GlobalScheduler 无公开 destroy，仅关闭 executor 以释放虚拟线程
        }
        if (executor != null) {
            executor.destroy();
        }
    }

    @Test
    @DisplayName("基线：裸 Runnable 循环执行（无框架）")
    void baseline_rawRunnableLoop() {
        AtomicLong counter = new AtomicLong(0);
        Runnable noop = () -> counter.incrementAndGet();

        warmupRaw(noop, WARMUP_COUNT);

        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_COUNT; i++) {
            noop.run();
        }
        long elapsedNs = System.nanoTime() - start;

        assertTrue(counter.get() >= WARMUP_COUNT + MEASURE_COUNT);
        report("baseline_rawRunnableLoop", MEASURE_COUNT, elapsedNs);
    }

    @Test
    @DisplayName("KeyedVirtualExecutor.execute 单链（SYSTEM）")
    void execute_singleChain() throws InterruptedException {
        AtomicLong counter = new AtomicLong(0);
        CountDownLatch warmupDone = new CountDownLatch(WARMUP_COUNT);
        warmupExecuteSingleChain(warmupDone, () -> {
            counter.incrementAndGet();
            warmupDone.countDown();
        }, WARMUP_COUNT);

        CountDownLatch done = new CountDownLatch(MEASURE_COUNT);
        Runnable task = () -> {
            counter.incrementAndGet();
            done.countDown();
        };
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_COUNT; i++) {
            executor.execute(TaskModule.SYSTEM, task);
        }
        assertTrue(done.await(60, TimeUnit.SECONDS));
        long elapsedNs = System.nanoTime() - start;

        assertTrue(counter.get() >= MEASURE_COUNT);
        report("execute_singleChain", MEASURE_COUNT, elapsedNs);
    }

    @Test
    @DisplayName("KeyedVirtualExecutor.execute 多链同 key（PLAYER, 1L）")
    void execute_multiChainSameKey() throws InterruptedException {
        AtomicLong counter = new AtomicLong(0);
        CountDownLatch warmupDone = new CountDownLatch(WARMUP_COUNT);
        warmupExecuteMultiChainSameKey(warmupDone, () -> {
            counter.incrementAndGet();
            warmupDone.countDown();
        }, WARMUP_COUNT);

        CountDownLatch done = new CountDownLatch(MEASURE_COUNT);
        Runnable task = () -> {
            counter.incrementAndGet();
            done.countDown();
        };
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_COUNT; i++) {
            executor.execute(TaskModule.PLAYER, 1L, task);
        }
        assertTrue(done.await(60, TimeUnit.SECONDS));
        long elapsedNs = System.nanoTime() - start;

        assertTrue(counter.get() >= MEASURE_COUNT);
        report("execute_multiChainSameKey", MEASURE_COUNT, elapsedNs);
    }

    @Test
    @DisplayName("KeyedVirtualExecutor.execute 多链多 key（PLAYER, 1..MULTI_KEY_COUNT）")
    void execute_multiChainManyKeys() throws InterruptedException {
        AtomicLong counter = new AtomicLong(0);
        CountDownLatch warmupDone = new CountDownLatch(WARMUP_COUNT);
        warmupExecuteMultiChainManyKeys(warmupDone, () -> {
            counter.incrementAndGet();
            warmupDone.countDown();
        }, WARMUP_COUNT);

        CountDownLatch done = new CountDownLatch(MEASURE_COUNT);
        Runnable task = () -> {
            counter.incrementAndGet();
            done.countDown();
        };
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_COUNT; i++) {
            long id = 1 + (i % MULTI_KEY_COUNT);
            executor.execute(TaskModule.PLAYER, id, task);
        }
        assertTrue(done.await(60, TimeUnit.SECONDS));
        long elapsedNs = System.nanoTime() - start;

        assertTrue(counter.get() >= MEASURE_COUNT);
        report("execute_multiChainManyKeys", MEASURE_COUNT, elapsedNs);
    }

    @Test
    @DisplayName("KeyedVirtualExecutor.submit 单链（CompletableFuture 开销）")
    void submit_singleChain() throws Exception {
        AtomicLong counter = new AtomicLong(0);
        Runnable task = () -> counter.incrementAndGet();

        warmupSubmitSingleChain(task, WARMUP_COUNT);

        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_COUNT; i++) {
            executor.submit(TaskModule.SYSTEM, task).get(10, TimeUnit.SECONDS);
        }
        long elapsedNs = System.nanoTime() - start;

        assertTrue(counter.get() >= MEASURE_COUNT);
        report("submit_singleChain", MEASURE_COUNT, elapsedNs);
    }

    @Test
    @DisplayName("KeyedVirtualExecutor.submit 多链多 key（CompletableFuture + 多 key）")
    void submit_multiChainManyKeys() throws Exception {
        AtomicLong counter = new AtomicLong(0);
        Runnable task = () -> counter.incrementAndGet();

        warmupSubmitMultiChainManyKeys(task, WARMUP_COUNT);

        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_COUNT; i++) {
            long id = 1 + (i % MULTI_KEY_COUNT);
            executor.submit(TaskModule.PLAYER, id, task).get(10, TimeUnit.SECONDS);
        }
        long elapsedNs = System.nanoTime() - start;

        assertTrue(counter.get() >= MEASURE_COUNT);
        report("submit_multiChainManyKeys", MEASURE_COUNT, elapsedNs);
    }

    @Test
    @DisplayName("GlobalScheduler.schedule(0) 多链多 key（调度器 + 转投执行器）")
    void schedule_zeroDelay_multiChainManyKeys() throws InterruptedException {
        int warmupN = Math.min(WARMUP_COUNT, 1000);
        AtomicLong counter = new AtomicLong(0);
        CountDownLatch warmupDone = new CountDownLatch(warmupN);
        warmupScheduleZeroDelay(warmupDone, () -> {
            counter.incrementAndGet();
            warmupDone.countDown();
        }, warmupN);

        CountDownLatch done = new CountDownLatch(MEASURE_COUNT);
        Runnable task = () -> {
            counter.incrementAndGet();
            done.countDown();
        };
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_COUNT; i++) {
            long id = 1 + (i % MULTI_KEY_COUNT);
            scheduler.schedule(TaskModule.PLAYER, id, task, 0, TimeUnit.NANOSECONDS);
        }
        assertTrue(done.await(90, TimeUnit.SECONDS));
        long elapsedNs = System.nanoTime() - start;

        assertTrue(counter.get() >= MEASURE_COUNT);
        report("schedule_zeroDelay_multiChainManyKeys", MEASURE_COUNT, elapsedNs);
    }

    // ---------- warmup helpers ----------

    private void warmupRaw(Runnable noop, int n) {
        for (int i = 0; i < n; i++) {
            noop.run();
        }
    }

    private void warmupExecuteSingleChain(CountDownLatch done, Runnable task, int n) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            executor.execute(TaskModule.SYSTEM, task);
        }
        done.await(30, TimeUnit.SECONDS);
    }

    private void warmupExecuteMultiChainSameKey(CountDownLatch done, Runnable task, int n) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            executor.execute(TaskModule.PLAYER, 1L, task);
        }
        done.await(30, TimeUnit.SECONDS);
    }

    private void warmupExecuteMultiChainManyKeys(CountDownLatch done, Runnable task, int n) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            executor.execute(TaskModule.PLAYER, 1 + (i % MULTI_KEY_COUNT), task);
        }
        done.await(30, TimeUnit.SECONDS);
    }

    private void warmupSubmitSingleChain(Runnable task, int n) throws Exception {
        for (int i = 0; i < n; i++) {
            executor.submit(TaskModule.SYSTEM, task).get(10, TimeUnit.SECONDS);
        }
    }

    private void warmupSubmitMultiChainManyKeys(Runnable task, int n) throws Exception {
        for (int i = 0; i < n; i++) {
            executor.submit(TaskModule.PLAYER, 1 + (i % MULTI_KEY_COUNT), task).get(10, TimeUnit.SECONDS);
        }
    }

    private void warmupScheduleZeroDelay(CountDownLatch done, Runnable task, int n) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            scheduler.schedule(TaskModule.PLAYER, 1 + (i % MULTI_KEY_COUNT), task, 0, TimeUnit.NANOSECONDS);
        }
        done.await(30, TimeUnit.SECONDS);
    }

    private static void report(String scenario, int count, long elapsedNs) {
        long elapsedMs = elapsedNs / 1_000_000;
        double opsPerSec = count * 1_000_000_000.0 / Math.max(1, elapsedNs);
        double avgUsPerTask = elapsedNs / 1000.0 / Math.max(1, count);
        System.out.printf("[ExecutorPerf] %s: count=%d, total=%d ms, throughput=%.0f ops/s, avg=%.2f μs/task%n",
                scenario, count, elapsedMs, opsPerSec, avgUsPerTask);
    }
}
