package com.slg.net.rpc.route.redis;

import com.slg.common.log.LoggerUtil;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 转发专用 Redis 自动配置
 * 根据 rpc.route.redis.* 配置创建独立的 Redis 连接工厂和 RedisTemplate
 * 与业务 Redis（spring.data.redis.*）完全隔离，互不干扰
 *
 * @author yangxunan
 * @date 2026/03/04
 */
@Configuration
@EnableConfigurationProperties(RpcRouteRedisProperties.class)
public class RouteRedisAutoConfiguration {

    /**
     * 转发专用 Redis 连接工厂（独立于业务 Redis）
     *
     * @param properties rpc.route.redis.* 配置
     * @return Lettuce 连接工厂
     */
    @Bean("routeRedisConnectionFactory")
    public RedisConnectionFactory routeRedisConnectionFactory(RpcRouteRedisProperties properties) {
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(properties.getHost());
        standaloneConfig.setPort(properties.getPort());
        standaloneConfig.setDatabase(properties.getDatabase());
        if (StringUtils.hasText(properties.getPassword())) {
            standaloneConfig.setPassword(properties.getPassword());
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(properties.getTimeout()))
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(properties.getTimeout()))
                                .build())
                        .build())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
        factory.afterPropertiesSet();

        LoggerUtil.info("[RpcRoute] 转发 Redis 连接工厂已创建: {}:{}/{}",
                properties.getHost(), properties.getPort(), properties.getDatabase());
        return factory;
    }

    /**
     * 转发专用 RedisTemplate
     * Key 使用 String 序列化，Value 使用 ByteArray 序列化（用于 Stream 二进制消息）
     *
     * @param routeRedisConnectionFactory 转发 Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean("routeRedisTemplate")
    public RedisTemplate<String, byte[]> routeRedisTemplate(
            RedisConnectionFactory routeRedisConnectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(routeRedisConnectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        RedisSerializer<byte[]> byteArraySerializer = RedisSerializer.byteArray();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(byteArraySerializer);
        template.setHashValueSerializer(byteArraySerializer);
        template.afterPropertiesSet();

        LoggerUtil.debug("[RpcRoute] 转发 Redis Template 已创建（ByteArray 序列化）");
        return template;
    }
}
