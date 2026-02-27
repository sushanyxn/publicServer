package com.slg.net.socket.server;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.config.WebSocketServerProperties;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import com.slg.net.socket.manager.WebSocketConnectionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket 服务端
 *
 * @author yangxunan
 * @date 2025-12-25
 */
public class WebSocketServer {

    private final String serverName;
    private final WebSocketServerProperties properties;
    private final WebSocketConnectionManager connectionManager;
    private final WebSocketMessageHandler messageHandler;
    private final ChannelHandler customChildHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public WebSocketServer(
            String serverName,
            WebSocketServerProperties properties,
            WebSocketConnectionManager connectionManager,
            WebSocketMessageHandler messageHandler) {
        this(serverName, properties, connectionManager, messageHandler, null);
    }

    /**
     * 支持自定义 ChannelInitializer 的构造函数
     * 当 customChildHandler 不为 null 时，使用自定义 Pipeline 替代默认的 WebSocketServerChannelInitializer
     *
     * @param customChildHandler 自定义的 ChannelInitializer，为 null 时使用默认
     */
    public WebSocketServer(
            String serverName,
            WebSocketServerProperties properties,
            WebSocketConnectionManager connectionManager,
            WebSocketMessageHandler messageHandler,
            ChannelHandler customChildHandler) {
        this.serverName = serverName;
        this.properties = properties;
        this.connectionManager = connectionManager;
        this.messageHandler = messageHandler;
        this.customChildHandler = customChildHandler;
    }

    /**
     * 启动服务器
     */
    public void start() {

        bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());

        try {
            ChannelHandler childHandler = customChildHandler != null
                    ? customChildHandler
                    : new WebSocketServerChannelInitializer(properties, messageHandler, connectionManager);

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(childHandler);

            ChannelFuture future = bootstrap.bind(properties.getPort()).sync();
            serverChannel = future.channel();

            LoggerUtil.info("{} WebSocket 服务端启动成功", serverName);
            LoggerUtil.info("端口: {}", properties.getPort());
            LoggerUtil.info("路径: {}", properties.getPath());
            LoggerUtil.info("Boss 线程数: {}", properties.getBossThreads());
            LoggerUtil.info("Worker 线程数: {}", properties.getWorkerThreads());

        } catch (Exception e) {
            LoggerUtil.error("WebSocket 服务端启动失败", e);
            shutdown();
            throw new RuntimeException("WebSocket 服务端启动失败", e);
        }
    }

    /**
     * 关闭服务器
     */
    @PreDestroy
    public void shutdown() {

        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }

        LoggerUtil.info("{} WebSocket 服务端开始关闭...", serverName);

        try {
            // 关闭所有连接
            if (connectionManager != null) {
                connectionManager.closeAll();
            }

            // 关闭服务器通道
            if (serverChannel != null) {
                serverChannel.close().sync();
            }

            if (workerGroup != null) {
                workerGroup.shutdownGracefully().await(5, java.util.concurrent.TimeUnit.SECONDS);
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully().await(5, java.util.concurrent.TimeUnit.SECONDS);
            }

            LoggerUtil.info("{} WebSocket 服务端已关闭", serverName);

        } catch (InterruptedException e) {
            // 在 ShutdownHook 中被中断是正常现象，不记录为错误
            LoggerUtil.warn("{} WebSocket 服务端关闭时被中断（正常现象）", serverName);
            // 恢复中断状态
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LoggerUtil.error("{} WebSocket 服务端关闭失败", serverName, e);
        }
    }

    /**
     * 获取连接管理器
     */
    public WebSocketConnectionManager getConnectionManager() {
        return connectionManager;
    }
}
