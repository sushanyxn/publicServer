package com.slg.robot;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.robot.core.config.RobotConfig;
import com.slg.robot.core.manager.RobotManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 机器人客户端生命周期管理
 * 基于 SmartLifecycle 实现，管理机器人客户端的启动和关闭流程
 * 在 JVM Shutdown Hook 触发 Spring 容器关闭时，stop() 方法会被调用
 *
 * @author yangxunan
 * @date 2026/01/22
 */
@Component
public class RobotClientLifeCycle implements SmartLifecycle {

    @Autowired
    private RobotConfig robotConfig;

    @Autowired
    private RobotManager robotManager;

    /**
     * 客户端是否正在运行
     */
    private volatile boolean running = false;

    /**
     * 客户端是否正在关闭（防止重复关闭）
     */
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    /**
     * SmartLifecycle 启动回调
     * 在所有 Bean 初始化完成后自动调用
     */
    @Override
    public void start() {
        LoggerUtil.debug("机器人客户端启动流程开始");

        try {

            // 创建并启动机器人
            LoggerUtil.debug("开始创建机器人，服务器地址: {}", robotConfig.getServerUrl());
            
            // 创建单个机器人进行测试
            robotManager.createRobot(1, 1001);
            
            running = true;
            LoggerUtil.debug("机器人客户端启动流程完成");

        } catch (Exception e) {
            LoggerUtil.error("机器人客户端启动失败！", e);
            throw new RuntimeException("机器人客户端启动失败", e);
        }
    }

    /**
     * SmartLifecycle 停止回调
     * 在 Spring 容器关闭时自动调用（由 JVM Shutdown Hook 触发）
     */
    @Override
    public void stop() {
        if (!shuttingDown.compareAndSet(false, true)) {
            LoggerUtil.debug("关闭流程已在执行中，跳过重复调用");
            return;
        }

        LoggerUtil.debug("开始执行机器人客户端关闭流程");

        try {
            // 业务线程池由 KeyedVirtualExecutor/GlobalScheduler 的 @PreDestroy 统一管理

            running = false;
            LoggerUtil.debug("机器人客户端关闭流程完成");

        } catch (Exception e) {
            LoggerUtil.error("机器人客户端关闭过程中发生异常", e);
            throw new RuntimeException("机器人客户端关闭失败", e);
        }
    }

    /**
     * 是否正在运行
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 生命周期阶段
     * 机器人客户端应该在游戏服务器之后启动，在游戏服务器之前关闭
     */
    @Override
    public int getPhase() {
        return LifecyclePhase.GAME_INIT;
    }

    /**
     * 是否自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 停止回调（带超时）
     */
    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}

