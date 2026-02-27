package com.slg.net.message.core.config;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.manager.MessageRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * 消息注册中心初始化器
 * 在 Spring 容器启动时主动加载消息注册中心
 * 确保在应用启动阶段就能发现配置问题
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
@Configuration
public class MessageRegistryInitializer {
    

    /**
     * Spring 容器启动时执行初始化
     */
    @PostConstruct
    public void init() {
        LoggerUtil.info("开始初始化消息注册中心...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            MessageRegistry registry = MessageRegistry.getInstance();
            long duration = System.currentTimeMillis() - startTime;
            LoggerUtil.info("消息注册中心初始化成功，耗时: {}ms", duration);
        } catch (Exception e) {
            LoggerUtil.error("消息注册中心初始化失败，应用启动将被终止", e);
            // 抛出异常，阻止 Spring 容器启动
            throw new IllegalStateException("消息注册中心初始化失败", e);
        }
    }
}

