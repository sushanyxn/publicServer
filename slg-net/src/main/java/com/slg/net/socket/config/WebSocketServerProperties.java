package com.slg.net.socket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 服务端配置属性
 *
 * @author yangxunan
 * @date 2025-12-25
 */
@ConfigurationProperties(prefix = "websocket.server")
@Getter
@Setter
public class WebSocketServerProperties {

    /**
     * 监听端口
     */
    private int port = 8080;

    /**
     * WebSocket 路径
     */
    private String path = "/ws";

    /**
     * Boss 线程数
     */
    private int bossThreads = 1;

    /**
     * Worker 线程数
     */
    private int workerThreads = 4;

    /**
     * 最大帧大小（字节）
     */
    private int maxFrameSize = 65536;

    /**
     * 读空闲超时时间（秒）
     * 超过此时间未收到任何数据则关闭连接
     * 建议设为心跳间隔的 3 倍（容忍 2 次丢失）
     * 0 表示不启用超时检测
     */
    private int readerIdleTime = 90;

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

    /**
     * 应用层消息最大长度（字节）
     * 超过此长度的消息将被拒绝并断开连接
     */
    private int maxMessageLength = 65536;

    /**
     * TCP 连接积压队列大小
     * 影响服务器同时处理连接请求的能力
     */
    private int backlog = 4096;

}
