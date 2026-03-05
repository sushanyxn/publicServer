package com.slg.common.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalScheduler 单元测试（手动注入 KeyedVirtualExecutor）
 */
class GlobalSchedulerTest {

    private VirtualExecutorHolder virtualExecutorHolder;
    private KeyedVirtualExecutor keyedVirtualExecutor;
    private GlobalScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        virtualExecutorHolder = new VirtualExecutorHolder();
        virtualExecutorHolder.init();

        keyedVirtualExecutor = new KeyedVirtualExecutor();
        Field holderField = KeyedVirtualExecutor.class.getDeclaredField("virtualExecutorHolder");
        holderField.setAccessible(true);
        holderField.set(keyedVirtualExecutor, virtualExecutorHolder);
        keyedVirtualExecutor.init();

        scheduler = new GlobalScheduler();
        Field kveField = GlobalScheduler.class.getDeclaredField("keyedVirtualExecutor");
        kveField.setAccessible(true);
        kveField.set(scheduler, keyedVirtualExecutor);
        scheduler.init();
    }

    @AfterEach
    void tearDown() {
        if (keyedVirtualExecutor != null) {
            keyedVirtualExecutor.destroy();
        }
    }

    @Test
    void schedule_delay_runsTaskAfterDelay() throws InterruptedException {
        CountDownLatch ran = new CountDownLatch(1);
        ScheduledFuture<?> future = scheduler.schedule(TaskModule.PLAYER, 1L, ran::countDown, 50, TimeUnit.MILLISECONDS);
        assertNotNull(future);
        assertTrue(ran.await(2, TimeUnit.SECONDS));
    }

    @Test
    void scheduleWithFixedDelay_singleChain_runsTaskAfterDelay() throws InterruptedException {
        CountDownLatch ran = new CountDownLatch(1);
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(TaskModule.SCENE, () -> ran.countDown(), 0, 1, TimeUnit.SECONDS);
        assertNotNull(future);
        assertTrue(ran.await(2, TimeUnit.SECONDS));
        future.cancel(false);
    }
}
