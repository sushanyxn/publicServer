package com.slg.net.message.core.codec;

import com.slg.common.log.LoggerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;

/**
 * WebSocket Frame 到 ByteBuf 的解码器
 * 将 BinaryWebSocketFrame 转换为 ByteBuf，供后续的 MessageDecoder 处理
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class WebSocketFrameToByteBufDecoder extends MessageToMessageDecoder<WebSocketFrame> {
    
    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
        // 只处理二进制帧
        if (frame instanceof BinaryWebSocketFrame) {
            ByteBuf content = frame.content();
            // 增加引用计数，因为后续还需要使用
            out.add(content.retain());
        } else {
            LoggerUtil.debug("忽略非二进制 WebSocket 帧: {}", frame.getClass().getSimpleName());
        }
    }
}

