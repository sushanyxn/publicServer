package com.slg.net.thrift.config;

import com.slg.net.socket.config.WebSocketServerProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thrift 协议适配层配置属性
 *
 * @author yangxunan
 * @date 2026/02/26
 */
@ConfigurationProperties(prefix = "thrift.adapter")
@Getter
@Setter
public class ThriftAdapterProperties {

    /**
     * 是否启用 Thrift 适配层
     */
    private boolean enabled = false;

    /**
     * Thrift WebSocket 监听端口
     */
    private int port = 50002;

    /**
     * WebSocket 路径
     */
    private String path = "/ws";

    /**
     * Thrift 序列化协议类型: binary / compact
     */
    private String protocol = "binary";

    /**
     * Boss 线程数
     */
    private int bossThreads = 1;

    /**
     * Worker 线程数
     */
    private int workerThreads = 2;

    /**
     * 最大帧大小
     */
    private int maxFrameSize = 65536;

    /**
     * 读空闲超时（秒），0 表示不检测
     */
    private int readerIdleTime = 60;

    /**
     * 写空闲超时（秒），0 表示不检测
     */
    private int writerIdleTime = 0;

    /**
     * 全部空闲超时（秒），0 表示不检测
     */
    private int allIdleTime = 0;

    /**
     * 转换为 WebSocketServerProperties，复用 WebSocketServer 启动逻辑
     */
    public WebSocketServerProperties toWebSocketServerProperties() {
        WebSocketServerProperties props = new WebSocketServerProperties();
        props.setPort(port);
        props.setPath(path);
        props.setBossThreads(bossThreads);
        props.setWorkerThreads(workerThreads);
        props.setMaxFrameSize(maxFrameSize);
        props.setReaderIdleTime(readerIdleTime);
        props.setWriterIdleTime(writerIdleTime);
        props.setAllIdleTime(allIdleTime);
        return props;
    }
}
