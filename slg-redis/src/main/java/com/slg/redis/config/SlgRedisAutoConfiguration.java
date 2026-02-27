package com.slg.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.redis.util.RedisConnectionValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * SLG Redis 自动配置类
 * 在 Spring Boot RedisAutoConfiguration 之前加载，覆盖默认 RedisTemplate 使用 Jackson 序列化
 *
 * <p>自动注册：
 * <ul>
 *   <li>{@link RedisTemplate} — Key 使用 String 序列化，Value 使用 Jackson JSON 序列化</li>
 *   <li>{@link RedisConnectionValidator} — 连接校验</li>
 *   <li>{@link RedisLifeCycleConfiguration} — 启动时连接验证</li>
 * </ul>
 *
 * <p>连接工厂（{@link RedisConnectionFactory}）和 {@link StringRedisTemplate} 由 Spring Boot 自动配置创建，
 * 支持 standalone / cluster / sentinel 三种模式，通过 {@code spring.data.redis.*} 配置
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@AutoConfiguration(before = RedisAutoConfiguration.class)
@ConditionalOnClass(RedisOperations.class)
public class SlgRedisAutoConfiguration {

    /**
     * 自定义 RedisTemplate，替换 Spring Boot 默认的 JDK 序列化
     * Key 使用 String 序列化，Value 使用 Jackson JSON 序列化
     *
     * @param connectionFactory Redis 连接工厂（由 Spring Boot 自动创建）
     * @return RedisTemplate 实例
     */
    @Bean("redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = JsonUtil.getMapper().copy();
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        LoggerUtil.debug("RedisTemplate 创建完成，使用 Jackson JSON 序列化");
        return template;
    }
}
