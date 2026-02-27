package com.slg.net.message.core.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * ByteBuf 到 WebSocket Frame 的编码器
 * 将 ByteBuf 转换为 BinaryWebSocketFrame，用于 WebSocket 传输
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class ByteBufToWebSocketFrameEncoder extends MessageToMessageEncoder<ByteBuf> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // 将 ByteBuf 包装为 BinaryWebSocketFrame
        out.add(new BinaryWebSocketFrame(msg.retain()));
    }
}

