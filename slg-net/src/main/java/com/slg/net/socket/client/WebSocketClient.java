package com.slg.net.socket.client;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.codec.ByteBufToWebSocketFrameEncoder;
import com.slg.net.message.core.codec.MessageDecoder;
import com.slg.net.message.core.codec.MessageEncoder;
import com.slg.net.message.core.codec.WebSocketFrameToByteBufDecoder;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.config.WebSocketClientProperties;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler.ClientHandshakeStateEvent;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 客户端
 * 基于 Netty 的 WebSocketClientProtocolHandler 实现
 *
 * @author yangxunan
 * @date 2025-12-25
 */
public class WebSocketClient {

    private final URI uri;
    private final WebSocketClientProperties properties;
    private final WebSocketMessageHandler messageHandler;
    private final EventLoopGroup sharedWorkerGroup;

    private Channel channel;
    private volatile boolean connected = false;

    public WebSocketClient(
            String url,
            WebSocketClientProperties properties,
            WebSocketMessageHandler messageHandler,
            EventLoopGroup sharedWorkerGroup) {
        try {
            this.uri = new URI(url);
            this.properties = properties;
            this.messageHandler = messageHandler;
            this.sharedWorkerGroup = sharedWorkerGroup;
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的 WebSocket URL: " + url, e);
        }
    }

    /**
     * 连接服务器（使用默认超时）
     */
    public NetSession connect() {
        return connect(properties.getConnectTimeout());
    }

    /**
     * 连接服务器（指定连接超时）
     *
     * @param connectTimeoutMs 连接超时时间（毫秒）
     */
    public NetSession connect(int connectTimeoutMs) {
        try {
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? (uri.getScheme().equals("wss") ? 443 : 80) : uri.getPort();

            // 创建 WebSocket 握手器
            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    uri,
                    WebSocketVersion.V13,
                    null,
                    true,
                    new DefaultHttpHeaders(),
                    properties.getMaxFrameSize());

            // 创建握手 CompletableFuture
            CompletableFuture<Void> handshakeFuture = new CompletableFuture<>();

            Bootstrap bootstrap = new Bootstrap()
                    .group(sharedWorkerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP 编解码器
                            pipeline.addLast(new HttpClientCodec());

                            // HTTP 消息聚合器
                            pipeline.addLast(new HttpObjectAggregator(properties.getMaxFrameSize()));

                            // WebSocket 数据压缩
                            pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);

                            // 空闲检测（可选，放在 WebSocketClientProtocolHandler 之前以感知 Pong 帧）
                            if (properties.getReaderIdleTime() > 0
                                    || properties.getWriterIdleTime() > 0
                                    || properties.getAllIdleTime() > 0) {
                                pipeline.addLast(new IdleStateHandler(
                                        properties.getReaderIdleTime(),
                                        properties.getWriterIdleTime(),
                                        properties.getAllIdleTime(),
                                        TimeUnit.SECONDS));
                            }

                            // WebSocket 客户端协议处理器（自动处理握手）
                            pipeline.addLast(new WebSocketClientProtocolHandler(handshaker));

                            // === 入站处理器 ===
                            // WebSocket Frame 转 ByteBuf
                            pipeline.addLast("frameDecoder", new WebSocketFrameToByteBufDecoder());

                            // 消息解码器（ByteBuf 转消息对象）
                            pipeline.addLast("messageDecoder", new MessageDecoder());

                            // === 出站处理器（注意顺序！先添加的后执行）===
                            // ByteBuf 转 WebSocket Frame（先添加，后执行）
                            pipeline.addLast("frameEncoder", new ByteBufToWebSocketFrameEncoder());

                            // 消息编码器（后添加，先执行）
                            pipeline.addLast("messageEncoder", new MessageEncoder());

                            // 握手监听器
                            pipeline.addLast(new HandshakeListener(handshakeFuture, messageHandler));
                        }
                    });

            // 连接服务器
            LoggerUtil.info("开始连接==========");
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            channel = channelFuture.channel();

            // 等待握手完成
            handshakeFuture.join();
            connected = true;
            LoggerUtil.info("WebSocket 客户端连接成功 地址: {}", uri);

            // 创建 NetSession
            NetSession session = new NetSession(channel);

            // 监听连接关闭
            channel.closeFuture().addListener(f -> {
                connected = false;
                LoggerUtil.warn("WebSocket 客户端连接关闭");
            });

            return session;
        } catch (Exception e) {
            LoggerUtil.error("WebSocket 客户端连接失败", e);
            disconnect();
        }
        return null;
    }

    /**
     * 握手监听器
     * 监听 WebSocket 握手完成事件
     */
    private static class HandshakeListener extends ChannelInboundHandlerAdapter {

        private final CompletableFuture<Void> handshakeFuture;
        private final WebSocketMessageHandler messageHandler;

        public HandshakeListener(CompletableFuture<Void> handshakeFuture, 
                                WebSocketMessageHandler messageHandler) {
            this.handshakeFuture = handshakeFuture;
            this.messageHandler = messageHandler;
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

            if (evt == ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                // 握手完成
                handshakeFuture.complete(null);
                LoggerUtil.debug("WebSocket 握手完成");
                
                // 通知业务处理器连接成功
                messageHandler.onConnect(ctx);
            } else if (evt == ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
                // 握手超时
                LoggerUtil.error("WebSocket 握手超时");
                handshakeFuture.completeExceptionally(new IllegalStateException("WebSocket 握手超时"));
            } else if (evt instanceof IdleStateEvent idleEvent) {
                if (idleEvent.state() == IdleState.ALL_IDLE || idleEvent.state() == IdleState.WRITER_IDLE) {
                    ctx.writeAndFlush(new PingWebSocketFrame());
                } else if (idleEvent.state() == IdleState.READER_IDLE) {
                    LoggerUtil.warn("WebSocket 客户端读超时，关闭连接: {}", ctx.channel().id().asShortText());
                    ctx.close();
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            messageHandler.onDisconnect(ctx);
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 过滤 HTTP 消息（握手阶段）
            if (msg instanceof io.netty.handler.codec.http.HttpResponse) {
                // HTTP 响应直接忽略，由 WebSocketClientProtocolHandler 处理
                return;
            }
            
            // 传递业务消息给业务处理器
            messageHandler.onMessage(ctx, msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            handshakeFuture.completeExceptionally(cause);
            messageHandler.onError(ctx, cause);
            ctx.close();
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        LoggerUtil.debug("WebSocket 客户端开始断开连接...");
        connected = false;

        try {
            if (channel != null && channel.isOpen()) {
                channel.close().sync();
            }

            LoggerUtil.debug("WebSocket 客户端已断开连接");

        } catch (InterruptedException e) {
            // 在 ShutdownHook 中被中断是正常现象，不记录为错误
            LoggerUtil.warn("WebSocket 客户端断开连接时被中断（正常现象）");
            // 恢复中断状态
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LoggerUtil.error("WebSocket 客户端断开连接失败", e);
        }
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return connected && channel != null && channel.isActive();
    }
}
