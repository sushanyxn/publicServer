package com.slg.net.thrift.server;

import com.slg.net.message.core.codec.ByteBufToWebSocketFrameEncoder;
import com.slg.net.message.core.codec.WebSocketFrameToByteBufDecoder;
import com.slg.net.socket.config.WebSocketServerProperties;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import com.slg.net.socket.manager.WebSocketConnectionManager;
import com.slg.net.socket.server.WebSocketServerHandler;
import com.slg.net.thrift.codec.ThriftMessageDecoder;
import com.slg.net.thrift.codec.ThriftMessageEncoder;
import com.slg.net.thrift.converter.ThriftConverterRegistry;
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
 * Thrift 客户端专用 WebSocket 通道初始化器
 * 与默认 Pipeline 的差异仅在编解码层：使用 ThriftMessageDecoder/Encoder 替代 MessageDecoder/Encoder
 * 其余 WebSocket 握手、帧转换、业务处理完全复用
 *
 * @author yangxunan
 * @date 2026/02/26
 */
public class ThriftWebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final WebSocketServerProperties properties;
    private final WebSocketMessageHandler messageHandler;
    private final WebSocketConnectionManager connectionManager;
    private final ThriftConverterRegistry converterRegistry;
    private final String protocolType;

    public ThriftWebSocketChannelInitializer(
            WebSocketServerProperties properties,
            WebSocketMessageHandler messageHandler,
            WebSocketConnectionManager connectionManager,
            ThriftConverterRegistry converterRegistry,
            String protocolType) {
        this.properties = properties;
        this.messageHandler = messageHandler;
        this.connectionManager = connectionManager;
        this.converterRegistry = converterRegistry;
        this.protocolType = protocolType;
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

        // 空闲检测
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
        // WebSocket Frame 转 ByteBuf（复用现有）
        pipeline.addLast(new WebSocketFrameToByteBufDecoder());

        // Thrift 消息解码器（替代 MessageDecoder）
        pipeline.addLast(new ThriftMessageDecoder(converterRegistry, protocolType));

        // === 出站处理器 ===
        // ByteBuf 转 WebSocket Frame（复用现有）
        pipeline.addLast(new ByteBufToWebSocketFrameEncoder());

        // Thrift 消息编码器（替代 MessageEncoder）
        pipeline.addLast(new ThriftMessageEncoder(converterRegistry, protocolType));

        // 业务处理器（完全复用现有）
        pipeline.addLast(new WebSocketServerHandler(messageHandler, connectionManager));
    }
}
