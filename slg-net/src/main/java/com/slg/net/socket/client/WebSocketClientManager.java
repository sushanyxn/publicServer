package com.slg.net.socket.client;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.config.WebSocketClientProperties;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 客户端管理器
 * 管理共享的 EventLoopGroup 和所有客户端连接
 * 
 * @author yangxunan
 * @date 2026/1/21
 */
public class WebSocketClientManager {

    @Autowired
    @Qualifier("webSocketClientMessageHandler")
    private WebSocketMessageHandler clientMessageHandler;

    @Autowired
    private WebSocketClientProperties webSocketClientProperties;

    /**
     * 共享的 EventLoopGroup（所有客户端连接共享）
     */
    private EventLoopGroup sharedWorkerGroup;

    /**
     * 活跃的客户端集合
     */
    private final Set<WebSocketClient> activeClients = ConcurrentHashMap.newKeySet();

    /**
     * 是否已关闭标识，防止 Lifecycle.stop() 和 @PreDestroy 重复执行
     */
    private volatile boolean closed = false;

    @Getter
    private static WebSocketClientManager instance;

    @PostConstruct
    public void init() {
        instance = this;
        
        // 创建共享的 EventLoopGroup
        int workerThreads = webSocketClientProperties.getWorkerThreads();
        sharedWorkerGroup = new NioEventLoopGroup(workerThreads);
    }

    /**
     * 连接到 WebSocket 服务器（使用默认超时）
     * 
     * @param url WebSocket 服务器地址
     * @return NetSession
     */
    public NetSession connect(String url) {
        return connect(url, webSocketClientProperties.getConnectTimeout());
    }

    /**
     * 连接到 WebSocket 服务器（指定连接超时）
     *
     * @param url              WebSocket 服务器地址
     * @param connectTimeoutMs 连接超时时间（毫秒），覆盖默认配置
     * @return NetSession，连接失败返回 null
     */
    public NetSession connect(String url, int connectTimeoutMs) {
        WebSocketClient webSocketClient = new WebSocketClient(
                url, 
                webSocketClientProperties, 
                clientMessageHandler, 
                sharedWorkerGroup);
        
        // 记录活跃客户端
        activeClients.add(webSocketClient);
        
        NetSession session = webSocketClient.connect(connectTimeoutMs);
        
        // 连接失败则移除
        if (session == null) {
            activeClients.remove(webSocketClient);
        }
        
        return session;
    }

    /**
     * 获取活跃连接数
     */
    public int getActiveConnectionCount() {
        return activeClients.size();
    }

    /**
     * 关闭所有连接
     * 可被 GameInitLifeCycle.stop() 主动调用，也会被 @PreDestroy 兜底调用
     * closed 标识防止重复执行
     */
    @PreDestroy
    public void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        LoggerUtil.debug("WebSocketClientManager 开始关闭，当前活跃连接数: {}", activeClients.size());
        
        // 关闭所有活跃客户端
        for (WebSocketClient client : activeClients) {
            try {
                client.disconnect();
            } catch (Exception e) {
                LoggerUtil.error("关闭客户端连接失败", e);
            }
        }
        activeClients.clear();
        
        // 关闭共享的 EventLoopGroup
        if (sharedWorkerGroup != null && !sharedWorkerGroup.isShutdown()) {
            try {
                sharedWorkerGroup.shutdownGracefully().await(5, TimeUnit.SECONDS);
                LoggerUtil.debug("共享 EventLoopGroup 已关闭");
            } catch (InterruptedException e) {
                LoggerUtil.warn("关闭 EventLoopGroup 被中断", e);
                Thread.currentThread().interrupt();
            }
        }
        
        LoggerUtil.debug("WebSocketClientManager 关闭完成");
    }

    /**
     * 移除客户端（由客户端断开时调用）
     */
    void removeClient(WebSocketClient client) {
        activeClients.remove(client);
    }
}
