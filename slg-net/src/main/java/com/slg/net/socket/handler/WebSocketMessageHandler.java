package com.slg.net.socket.handler;

import io.netty.channel.ChannelHandlerContext;

/**
 * WebSocket 消息处理器接口
 * 业务层实现此接口来处理 WebSocket 消息
 *
 * @author yangxunan
 * @date 2025-12-25
 */
public interface WebSocketMessageHandler {

    /**
     * 连接建立时调用
     */
    void onConnect(ChannelHandlerContext ctx);

    /**
     * 收到消息时调用
     * 
     * @param ctx 通道上下文
     * @param message 解码后的消息对象
     */
    void onMessage(ChannelHandlerContext ctx, Object message);

    /**
     * 连接断开时调用
     */
    void onDisconnect(ChannelHandlerContext ctx);

    /**
     * 发生异常时调用
     */
    void onError(ChannelHandlerContext ctx, Throwable cause);
}
