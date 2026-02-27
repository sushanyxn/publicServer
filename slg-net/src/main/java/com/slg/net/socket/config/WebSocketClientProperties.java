package com.slg.net.socket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 客户端配置属性
 *
 * @author yangxunan
 * @date 2025-12-25
 */
@ConfigurationProperties(prefix = "websocket.client")
@Getter
@Setter
public class WebSocketClientProperties {

    /**
     * Worker 线程数
     */
    private int workerThreads = 2;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 最大帧大小（字节）
     */
    private int maxFrameSize = 65536;

    /**
     * 重连间隔（毫秒）
     */
    private int reconnectInterval = 3000;

    /**
     * 心跳间隔（秒）
     * 0 表示不发送心跳
     */
    private int heartbeatInterval = 30;

    /**
     * 读空闲超时时间（秒）
     * 0 表示不启用超时检测
     */
    private int readerIdleTime = 0;

    /**
     * 写空闲超时时间（秒）
     * 0 表示不启用超时检测
     */
    private int writerIdleTime = 0;

    /**
     * 所有空闲超时时间（秒）
     * 0 表示不启用超时检测
     */
    private int allIdleTime = 0;

}
