package com.slg.common.executor;

import com.slg.common.executor.core.KeyedVirtualExecutor;
import com.slg.common.executor.core.VirtualExecutorHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KeyedVirtualExecutor 单元测试（手动注入 VirtualExecutorHolder，无 Spring）
 */
class KeyedVirtualExecutorTest {

    private VirtualExecutorHolder virtualExecutorHolder;
    private KeyedVirtualExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        virtualExecutorHolder = new VirtualExecutorHolder();
        virtualExecutorHolder.init();

        executor = new KeyedVirtualExecutor();
        Field holderField = KeyedVirtualExecutor.class.getDeclaredField("virtualExecutorHolder");
        holderField.setAccessible(true);
        holderField.set(executor, virtualExecutorHolder);
        executor.init();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.destroy();
        }
    }

    @Test
    void execute_sameKey_runsSerially() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch firstDone = new CountDownLatch(1);

        executor.execute(TaskModule.PLAYER, 1L, () -> {
            firstStarted.countDown();
            counter.incrementAndGet();
            try {
                firstDone.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        executor.execute(TaskModule.PLAYER, 1L, () -> counter.addAndGet(10));

        assertTrue(firstStarted.await(3, TimeUnit.SECONDS));
        assertEquals(1, counter.get());
        firstDone.countDown();
        Thread.sleep(200);
        assertEquals(11, counter.get());
    }

    @Test
    void execute_differentKeys_runConcurrently() throws InterruptedException {
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        executor.execute(TaskModule.PLAYER, 1L, () -> {
            bothStarted.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        executor.execute(TaskModule.PLAYER, 2L, () -> {
            bothStarted.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(bothStarted.await(3, TimeUnit.SECONDS));
        release.countDown();
    }

    @Test
    void execute_singleChain_runsSerially() throws Exception {
        AtomicInteger order = new AtomicInteger(0);
        CompletableFuture<Void> f1 = executor.submit(TaskModule.SYSTEM, (Runnable) () -> order.compareAndSet(0, 1));
        CompletableFuture<Void> f2 = executor.submit(TaskModule.SYSTEM, (Runnable) () -> order.compareAndSet(1, 2));

        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        assertEquals(2, order.get());
    }

    @Test
    void inThread_insideTask_returnsTrue() throws Exception {
        CompletableFuture<Boolean> result = executor.submit(TaskModule.PLAYER, 99L,
                (java.util.concurrent.Callable<Boolean>) () -> executor.inThread(TaskModule.PLAYER, 99L));
        assertTrue(result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void execute_taskThrows_nextTaskStillRuns() throws InterruptedException {
        AtomicInteger secondRan = new AtomicInteger(0);
        executor.execute(TaskModule.PLAYER, 1L, () -> {
            throw new RuntimeException("expected");
        });
        executor.execute(TaskModule.PLAYER, 1L, () -> secondRan.incrementAndGet());

        Thread.sleep(500);
        assertEquals(1, secondRan.get());
    }
}
