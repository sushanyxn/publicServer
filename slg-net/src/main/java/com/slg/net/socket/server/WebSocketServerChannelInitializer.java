package com.slg.net.socket.server;

import com.slg.net.message.core.codec.ByteBufToWebSocketFrameEncoder;
import com.slg.net.message.core.codec.MessageDecoder;
import com.slg.net.message.core.codec.MessageEncoder;
import com.slg.net.message.core.codec.WebSocketFrameToByteBufDecoder;
import com.slg.net.socket.config.WebSocketServerProperties;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import com.slg.net.socket.manager.WebSocketConnectionManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket 服务端通道初始化器
 *
 * @author yangxunan
 * @date 2025-12-25
 */
public class WebSocketServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final WebSocketServerProperties properties;
    private final WebSocketMessageHandler messageHandler;
    private final WebSocketConnectionManager connectionManager;

    public WebSocketServerChannelInitializer(
            WebSocketServerProperties properties,
            WebSocketMessageHandler messageHandler,
            WebSocketConnectionManager connectionManager) {
        this.properties = properties;
        this.messageHandler = messageHandler;
        this.connectionManager = connectionManager;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // HTTP 编解码器
        pipeline.addLast(new HttpServerCodec());

        // 大数据流支持
        pipeline.addLast(new ChunkedWriteHandler());

        // HTTP 消息聚合器
        pipeline.addLast(new HttpObjectAggregator(65536));

        // 空闲检测（可选）
        if (properties.getReaderIdleTime() > 0 
                || properties.getWriterIdleTime() > 0 
                || properties.getAllIdleTime() > 0) {
            pipeline.addLast(new IdleStateHandler(
                    properties.getReaderIdleTime(),
                    properties.getWriterIdleTime(),
                    properties.getAllIdleTime(),
                    TimeUnit.SECONDS));
        }

        // WebSocket 协议处理器
        pipeline.addLast(new WebSocketServerProtocolHandler(
                properties.getPath(),
                null,
                true,
                properties.getMaxFrameSize()));

        // === 入站处理器 ===
        // WebSocket Frame 转 ByteBuf
        pipeline.addLast(new WebSocketFrameToByteBufDecoder());
        
        // 消息解码器（ByteBuf 转消息对象）
        pipeline.addLast(new MessageDecoder(properties.getMaxMessageLength()));
        
        // === 出站处理器（注意顺序！先添加的后执行）===
        // ByteBuf 转 WebSocket Frame（先添加，后执行）
        pipeline.addLast(new ByteBufToWebSocketFrameEncoder());
        
        // 消息编码器（后添加，先执行）
        pipeline.addLast(new MessageEncoder());

        // 业务处理器
        pipeline.addLast(new WebSocketServerHandler(messageHandler, connectionManager));
    }
}
