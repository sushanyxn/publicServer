package com.slg.net.rpc.config;

import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.handler.InnerSessionDisconnectListener;
import com.slg.net.rpc.handler.RpcServerMessageHandler;
import com.slg.net.socket.config.WebSocketConnectionManagerConfiguration;
import com.slg.net.socket.manager.WebSocketConnectionManager;
import com.slg.net.socket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * RPC 服务端配置类
 * 
 * @author yangxunan
 * @date 2026/01/26
 */
@Configuration
@EnableConfigurationProperties(RpcServerProperties.class)
@Import(WebSocketConnectionManagerConfiguration.class)
public class RpcServerConfiguration {

    /**
     * 内部连接断线监听器（可选，由业务模块提供实现）
     */
    @Autowired(required = false)
    private InnerSessionDisconnectListener disconnectListener;

    /**
     * RPC 服务器消息处理器
     */
    @Bean(name = "rpcServerMessageHandler")
    public RpcServerMessageHandler rpcServerMessageHandler() {
        LoggerUtil.debug("[RPC Server] 创建 RPC 服务器消息处理器, disconnectListener={}", disconnectListener);
        return new RpcServerMessageHandler(disconnectListener);
    }

    /**
     * RPC WebSocket 服务器
     * 使用共享的连接管理器
     */
    @Bean(name = "rpcServer")
    public WebSocketServer rpcServer(
            RpcServerProperties rpcProperties,
            RpcServerMessageHandler rpcServerMessageHandler,
            WebSocketConnectionManager connectionManager) {
        LoggerUtil.debug("[RPC Server] 创建 RPC WebSocket 服务器，使用共享连接管理器");
        return new WebSocketServer(
                "RPC",
                rpcProperties.toWebSocketServerProperties(),
                connectionManager,
                rpcServerMessageHandler);
    }
}
