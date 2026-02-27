package com.slg.redis.config;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.redis.util.RedisConnectionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Redis 生命周期管理
 * 负责在服务启动时验证 Redis 连接，确保缓存服务可用
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class RedisLifeCycleConfiguration implements SmartLifecycle {

    @Autowired
    private RedisConnectionValidator validator;

    private volatile boolean running = false;

    @Override
    public void start() {
        if (!validator.validateConnection()) {
            LoggerUtil.error("Redis 连接失败，服务器启动终止！");
            throw new RuntimeException("Redis 连接失败");
        }
        running = true;
        LoggerUtil.debug("Redis 生命周期启动完成");
    }

    @Override
    public void stop() {
        running = false;
        LoggerUtil.debug("Redis 生命周期关闭完成");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.REDIS;
    }
}
