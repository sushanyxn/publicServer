package com.slg.redis.util;

import com.slg.common.log.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 连接校验器
 * 通过执行 PING 命令验证 Redis 连接是否正常
 * 兼容 standalone / cluster / sentinel 三种模式
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class RedisConnectionValidator {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 验证 Redis 连接
     * 通过 RedisTemplate 执行 PING 命令，兼容所有连接模式
     *
     * @return true 连接正常，false 连接失败
     */
    public boolean validateConnection() {
        try {
            String pong = stringRedisTemplate.execute((RedisCallback<String>) RedisConnection::ping);
            LoggerUtil.debug("Redis 连接验证成功: PING -> {}", pong);
            return "PONG".equals(pong);
        } catch (Exception e) {
            LoggerUtil.error("Redis 连接验证失败", e);
            return false;
        }
    }
}
