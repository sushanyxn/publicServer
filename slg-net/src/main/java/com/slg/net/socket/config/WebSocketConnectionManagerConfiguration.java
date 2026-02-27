package com.slg.net.socket.config;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.manager.WebSocketConnectionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket 连接管理器配置
 * 提供共享的连接管理器，供游戏服务器和 RPC 服务器共同使用
 * 
 * @author yangxunan
 * @date 2026/01/26
 */
@Configuration
public class WebSocketConnectionManagerConfiguration {

    /**
     * WebSocket 连接管理器（单例，共享）
     * 无论启用哪个服务器，都使用同一个连接管理器
     */
    @Bean
    @ConditionalOnMissingBean(WebSocketConnectionManager.class)
    public WebSocketConnectionManager webSocketConnectionManager() {
        LoggerUtil.info("[WebSocket] 创建共享连接管理器");
        return new WebSocketConnectionManager();
    }
}

