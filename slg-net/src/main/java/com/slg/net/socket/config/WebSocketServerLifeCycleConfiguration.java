package com.slg.net.socket.config;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket 服务器生命周期配置
 * 通过 @EnableWebSocketServer 注解自动引入
 * 
 * @author yangxunan
 * @date 2026/01/28
 */
@Configuration
public class WebSocketServerLifeCycleConfiguration {
    
    @Bean
    public SmartLifecycle webSocketServerLifeCycle(
            @Qualifier("webSocketServer") WebSocketServer webSocketServer) {
        return new SmartLifecycle() {
            
            private volatile boolean running = false;
            
            @Override
            public void start() {
                LoggerUtil.info("游戏服务器启动流程开始");
                
                // 启动 WebSocket 服务器
                webSocketServer.start();
                
                running = true;
                LoggerUtil.info("游戏服务器启动流程完成");
            }
            
            @Override
            public void stop() {
                LoggerUtil.info("开始执行游戏服务器关闭流程");
                
                // 关闭游戏服务器
                webSocketServer.shutdown();
                
                running = false;
                LoggerUtil.info("游戏服务器关闭流程完成");
            }
            
            @Override
            public boolean isRunning() {
                return running;
            }
            
            @Override
            public int getPhase() {
                return LifecyclePhase.WEBSOCKET_SERVER;
            }
        };
    }
}
