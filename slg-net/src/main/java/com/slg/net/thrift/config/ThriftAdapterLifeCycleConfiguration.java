package com.slg.net.thrift.config;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thrift 适配层生命周期配置
 * 控制 Thrift WebSocket 服务器的启动和关闭时机
 *
 * @author yangxunan
 * @date 2026/02/26
 */
@Configuration
@ConditionalOnProperty(name = "thrift.adapter.enabled", havingValue = "true")
public class ThriftAdapterLifeCycleConfiguration {

    @Bean
    public SmartLifecycle thriftServerLifeCycle(
            @Qualifier("thriftServer") WebSocketServer thriftServer) {
        return new SmartLifecycle() {

            private volatile boolean running = false;

            @Override
            public void start() {
                LoggerUtil.debug("Thrift 适配层启动流程开始");
                thriftServer.start();
                running = true;
                LoggerUtil.debug("Thrift 适配层启动流程完成");
            }

            @Override
            public void stop() {
                LoggerUtil.debug("开始执行 Thrift 适配层关闭流程");
                thriftServer.shutdown();
                running = false;
                LoggerUtil.debug("Thrift 适配层关闭流程完成");
            }

            @Override
            public boolean isRunning() {
                return running;
            }

            @Override
            public int getPhase() {
                return LifecyclePhase.THRIFT_ADAPTER;
            }
        };
    }
}
