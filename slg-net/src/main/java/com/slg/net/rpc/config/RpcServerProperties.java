package com.slg.net.rpc.config;

import com.slg.net.socket.config.WebSocketServerProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RPC 服务端配置属性
 *
 * @author yangxunan
 * @date 2026/01/26
 */
@ConfigurationProperties(prefix = "rpc.server")
@Getter
@Setter
public class RpcServerProperties {

    /**
     * 监听端口
     */
    private int port = 51001;

    /**
     * WebSocket 路径
     */
    private String path = "/rpc";

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
    private int allIdleTime = 300;

    /**
     * 转换为 WebSocketServerProperties
     * 用于创建 WebSocketServer 实例
     */
    public WebSocketServerProperties toWebSocketServerProperties() {
        WebSocketServerProperties props = new WebSocketServerProperties();
        props.setPort(this.port);
        props.setPath(this.path);
        props.setBossThreads(this.bossThreads);
        props.setWorkerThreads(this.workerThreads);
        props.setMaxFrameSize(this.maxFrameSize);
        props.setReaderIdleTime(this.readerIdleTime);
        props.setWriterIdleTime(this.writerIdleTime);
        props.setAllIdleTime(this.allIdleTime);
        return props;
    }

}

