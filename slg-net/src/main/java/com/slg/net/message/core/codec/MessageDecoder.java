package com.slg.net.message.core.codec;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.manager.MessageRegistry;
import com.slg.net.message.core.model.MessageFieldMeta;
import com.slg.net.message.core.model.MessageMeta;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;

import java.util.List;

/**
 * 消息解码器
 * 将字节流解码为消息对象
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
 * Body:   消息体，按字段字典序反序列化
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class MessageDecoder extends ByteToMessageDecoder {
    
    private static final MessageRegistry registry = MessageRegistry.getInstance();

    /**
     * 内部 RPC 通信默认的消息长度上限（1MB）
     */
    private static final int DEFAULT_MAX_MESSAGE_LENGTH = 1048576;
    
    private final int maxMessageLength;

    public MessageDecoder() {
        this(DEFAULT_MAX_MESSAGE_LENGTH);
    }

    /**
     * @param maxMessageLength 消息最大长度（字节），超过此长度的消息将被拒绝
     */
    public MessageDecoder(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.markReaderIndex();
        
        // 1. 尝试读取 Length (VarInt)，处理粘包
        if (!isVarIntReadable(in)) {
            in.resetReaderIndex();
            return;
        }
        int length = VarIntCodec.readVarInt(in);
        
        // 2. 消息长度合法性检查
        if (length <= 0 || length > maxMessageLength) {
            LoggerUtil.error("消息长度异常: length={}, max={}, remote={}",
                    length, maxMessageLength, ctx.channel().remoteAddress());
            ctx.close();
            throw new DecoderException("消息长度异常: " + length);
        }
        
        // 3. 检查消息体是否完整
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        
        // 4. 创建有界视图，隔离消息边界
        // readSlice 会推进 in 的 readerIndex，即使 msgBuf 消费异常也不会影响后续消息
        ByteBuf msgBuf = in.readSlice(length);
        
        // 5. 读取 MsgId (VarInt)
        int msgId = VarIntCodec.readVarInt(msgBuf);

        // 6. 查询类型和 Meta
        Class<?> clazz = registry.getClass(msgId);
        if (clazz == null) {
            LoggerUtil.error("未知的协议号: {}, remote={}", msgId, ctx.channel().remoteAddress());
            return;
        }
        
        MessageMeta meta = registry.getMeta(clazz);
        if (meta == null) {
            LoggerUtil.error("未找到 Meta: {}", clazz.getName());
            return;
        }
        
        if (!meta.isInstantiable()) {
            LoggerUtil.error("无法实例化抽象类/接口: {}", clazz.getName());
            return;
        }
        
        try {
            Object instance = meta.getConstructor().newInstance();
            
            for (MessageFieldMeta field : meta.getFields()) {
                Object value = MessageCodec.readValue(msgBuf);
                field.getSetter().invoke(instance, MessageCodec.adaptValue(value, field.getFieldType()));
            }
            
            if (msgBuf.readableBytes() > 0) {
                LoggerUtil.warn("消息未完全消费: msgId={}, class={}, 剩余 {} 字节",
                        msgId, clazz.getSimpleName(), msgBuf.readableBytes());
            }
            
            out.add(instance);
        } catch (Throwable e) {
            LoggerUtil.error("消息解码失败，协议号: {}, 类型: {}", msgId, clazz.getName(), e);
        }
    }
    
    /**
     * 检查是否可以完整读取一个 VarInt
     * 避免读取不完整的 VarInt 导致状态错误
     * 
     * VarInt 编码规则：每字节最高位为继续标志
     * - 1: 后面还有字节
     * - 0: 这是最后一个字节
     * 
     * @param buf 缓冲区
     * @return true 如果可以完整读取，false 如果数据不完整
     */
    private boolean isVarIntReadable(ByteBuf buf) {
        int readableBytes = buf.readableBytes();
        if (readableBytes == 0) {
            return false;
        }
        
        int readerIndex = buf.readerIndex();
        for (int i = 0; i < Math.min(5, readableBytes); i++) {
            byte b = buf.getByte(readerIndex + i);
            if ((b & 0x80) == 0) {
                return true;
            }
        }
        
        return readableBytes >= 5;
    }
}
