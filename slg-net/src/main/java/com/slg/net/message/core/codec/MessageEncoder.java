package com.slg.net.message.core.codec;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.manager.MessageRegistry;
import com.slg.net.message.core.model.MessageFieldMeta;
import com.slg.net.message.core.model.MessageMeta;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * 消息编码器
 * 将消息对象编码为字节流
 * 
 * 协议格式：
 * +----------+----------+--------+
 * | Length   | MsgId    |  Body  |
 * +----------+----------+--------+
 * | VarInt   | VarInt   | 变长   |
 * +----------+----------+--------+
 * 
 * Length: 消息总长度（MsgId + Body），使用 VarInt 编码
 * MsgId:  协议号，使用 VarInt 编码（总是非负数）
 * Body:   消息体，按字段字典序序列化
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class MessageEncoder extends MessageToByteEncoder<Object> {
    
    private static final MessageRegistry registry = MessageRegistry.getInstance();
    
    /**
     * 判断消息是否应该被此编码器处理
     * 返回 false 的消息会被直接传递给下一个处理器
     */
    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        if (msg instanceof HttpObject || msg instanceof WebSocketFrame) {
            return false;
        }
        return super.acceptOutboundMessage(msg);
    }
    
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        // 1. 获取实际类型
        Class<?> actualType = msg.getClass();

        // 2. 查询协议号和 Meta
        Integer msgId = registry.getProtocolId(actualType);
        if (msgId == null) {
            LoggerUtil.error("未注册的消息类型: {}", actualType.getName());
            throw new IllegalStateException("未注册的消息类型: " + actualType.getName());
        }
        
        MessageMeta meta = registry.getMeta(actualType);
        if (meta == null) {
            LoggerUtil.error("未找到 Meta: {}", actualType.getName());
            throw new IllegalStateException("未找到 Meta: " + actualType.getName());
        }
        
        try {
            // 3. 创建临时 ByteBuf 序列化消息体
            ByteBuf bodyBuf = ctx.alloc().buffer();
            
            try {
                // 4. 按字典序序列化字段
                for (MessageFieldMeta field : meta.getFields()) {
                    Object value = field.getGetter().invoke(msg);
                    MessageCodec.writeValue(bodyBuf, value);
                }
                
                // 5. 计算长度并写入完整消息
                int bodyLength = bodyBuf.readableBytes();
                int msgIdLength = VarIntCodec.varIntSize(msgId);  // MsgId 的 VarInt 字节数
                int totalLength = msgIdLength + bodyLength;       // 总长度 = MsgId长度 + Body长度
                
                VarIntCodec.writeVarInt(out, totalLength);  // Length (VarInt)
                VarIntCodec.writeVarInt(out, msgId);        // MsgId (VarInt, 协议号总是非负)
                out.writeBytes(bodyBuf);                     // Body
            } finally {
                // 释放临时缓冲区
                bodyBuf.release();
            }
        } catch (Throwable e) {
            LoggerUtil.error("消息编码失败，类型: {}", actualType.getName(), e);
            throw new IllegalStateException("消息编码失败: " + actualType.getName(), e);
        }
    }
}

