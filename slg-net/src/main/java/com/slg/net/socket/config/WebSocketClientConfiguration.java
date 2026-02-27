package com.slg.net.socket.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket 客户端配置类
 * 只启用配置属性，不自动创建客户端 Bean
 * 使用方可以手动创建 WebSocketClient 实例
 * 
 * @author yangxunan
 * @date 2025-12-25
 */
@Configuration
@EnableConfigurationProperties(WebSocketClientProperties.class)
public class WebSocketClientConfiguration {
    // 仅用于加载配置，不创建任何 Bean
}
