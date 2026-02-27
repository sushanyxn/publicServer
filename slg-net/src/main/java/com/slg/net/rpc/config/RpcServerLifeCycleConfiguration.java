package com.slg.net.rpc.config;

import com.slg.common.constant.LifecyclePhase;
import com.slg.net.socket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RPC 服务器生命周期配置
 * 通过 @EnableRpcServer 注解自动引入
 * 
 * @author yangxunan
 * @date 2026/01/28
 */
@Configuration
public class RpcServerLifeCycleConfiguration {
    
    @Bean
    public SmartLifecycle rpcServerLifeCycle(
            @Qualifier("rpcServer") WebSocketServer rpcServer) {
        return new SmartLifecycle() {
            
            private volatile boolean running = false;
            
            @Override
            public void start() {
                // 启动 RPC 服务器
                rpcServer.start();
                running = true;
            }
            
            @Override
            public void stop() {
                // 关闭rpc服务器
                rpcServer.shutdown();
                running = false;
            }
            
            @Override
            public boolean isRunning() {
                return running;
            }
            
            @Override
            public int getPhase() {
                return LifecyclePhase.RPC_SERVER;
            }
        };
    }
}
