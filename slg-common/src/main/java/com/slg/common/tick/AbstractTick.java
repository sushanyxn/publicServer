package com.slg.common.tick;

import com.slg.common.executor.GlobalScheduler;
import com.slg.common.executor.TaskModule;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 抽象定时任务基类
 * 使用 {@link GlobalScheduler} 的自递归调度模式，保证上一次 tick 执行完毕后才计时下一次延迟，避免任务堆积
 *
 * <p>子类需要实现：
 * <ul>
 *   <li>{@link #tick()} -- 定时执行的业务逻辑</li>
 *   <li>{@link #getTaskModule()} -- 任务执行所在的模块</li>
 *   <li>{@link #getTickTime()} -- tick 间隔时间（毫秒）</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
public abstract class AbstractTick {

    /**
     * 当前定时任务的 ScheduledFuture，用于取消
     */
    private ScheduledFuture<?> task;

    /**
     * 所有已注册的 tick 实例
     */
    public static Map<Class<? extends AbstractTick>, AbstractTick> ticks = new HashMap<>();

    /**
     * 启动所有已注册的 tick
     */
    public static void startAll() {
        for (AbstractTick tick : ticks.values()) {
            tick.start();
        }
    }

    /**
     * 停止所有已注册的 tick
     */
    public static void stopAll() {
        for (AbstractTick tick : ticks.values()) {
            tick.stop();
        }
    }

    /**
     * 注册 tick 实例
     */
    @PostConstruct
    public void init() {
        ticks.put(getClass(), this);
    }

    /**
     * 启动定时任务
     * 使用自递归调度模式：任务完成后才调度下一次，保证不堆积
     */
    public void start() {
        stop();
        long tickTime = getTickTime();
        if (tickTime <= 0) {
            throw new RuntimeException(String.format("[Tick] %s tick时间没有设置", getClass().getSimpleName()));
        }

        scheduleNext(getInitDelayTime());
    }

    /**
     * 调度下一次 tick（自递归模式）
     *
     * @param delay 延迟时间（毫秒）
     */
    private void scheduleNext(long delay) {
        task = GlobalScheduler.getInstance().schedule(getTaskModule(), () -> {
            try {
                tick();
            } finally {
                scheduleNext(getTickTime());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止定时任务
     */
    public void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    /**
     * 定时执行的业务逻辑
     */
    public abstract void tick();

    /**
     * 获取任务执行所在的模块
     *
     * @return 任务模块枚举
     */
    public abstract TaskModule getTaskModule();

    /**
     * 获取 tick 间隔时间（毫秒）
     *
     * @return 间隔时间
     */
    public abstract long getTickTime();

    /**
     * 获取初始延迟时间（毫秒），默认等于 tickTime
     *
     * @return 初始延迟时间
     */
    public long getInitDelayTime() {
        return getTickTime();
    }
}
