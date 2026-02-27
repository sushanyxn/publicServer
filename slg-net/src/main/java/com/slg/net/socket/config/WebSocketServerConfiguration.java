package com.slg.net.socket.config;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import com.slg.net.socket.manager.WebSocketConnectionManager;
import com.slg.net.socket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * WebSocket 服务端配置类
 * 
 * @author yangxunan
 * @date 2025-12-25
 */
@Configuration
@EnableConfigurationProperties(WebSocketServerProperties.class)
@Import(WebSocketConnectionManagerConfiguration.class)
public class WebSocketServerConfiguration {

    /**
     * WebSocket 服务端
     * 需要业务模块提供 webSocketServerMessageHandler Bean
     */
    @Bean
    public WebSocketServer webSocketServer(
            WebSocketServerProperties properties,
            WebSocketConnectionManager connectionManager,
            @Qualifier("webSocketServerMessageHandler") WebSocketMessageHandler messageHandler) {
        LoggerUtil.info("[Game Server] 创建游戏服务器，使用共享连接管理器");
        return new WebSocketServer("Game", properties, connectionManager, messageHandler);
    }
}
