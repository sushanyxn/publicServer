package com.slg.common.executor;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 可取消的自递归定时任务包装
 * 封装 {@link AtomicBoolean} 取消标志和 {@link AtomicReference} 当前调度引用，
 * 调用 {@link #cancel} 时同时设置标志并取消当前待执行的调度，阻止后续递归
 *
 * @author yangxunan
 * @date 2026/02/25
 */
public class CancellableScheduledFuture implements ScheduledFuture<Object> {

    /**
     * 当前（或下一次）调度的 ScheduledFuture 引用
     * 每次自递归调度后更新为新的 future
     */
    private final AtomicReference<ScheduledFuture<?>> currentFutureRef;

    /**
     * 取消标志，设为 true 后阻止 whenComplete 回调中的下一次调度
     */
    private final AtomicBoolean cancelled;

    public CancellableScheduledFuture(AtomicReference<ScheduledFuture<?>> currentFutureRef,
                                      AtomicBoolean cancelled) {
        this.currentFutureRef = currentFutureRef;
        this.cancelled = cancelled;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelled.set(true);
        ScheduledFuture<?> current = currentFutureRef.get();
        return current != null && current.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean isDone() {
        if (cancelled.get()) {
            return true;
        }
        ScheduledFuture<?> current = currentFutureRef.get();
        return current != null && current.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        ScheduledFuture<?> current = currentFutureRef.get();
        if (current != null) {
            return current.get();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        ScheduledFuture<?> current = currentFutureRef.get();
        if (current != null) {
            return current.get(timeout, unit);
        }
        return null;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        ScheduledFuture<?> current = currentFutureRef.get();
        if (current != null) {
            return current.getDelay(unit);
        }
        return 0;
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof CancellableScheduledFuture other) {
            return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
        }
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
    }
}
