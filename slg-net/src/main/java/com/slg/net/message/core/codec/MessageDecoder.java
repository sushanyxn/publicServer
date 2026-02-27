package com.slg.net.message.core.codec;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.manager.MessageRegistry;
import com.slg.net.message.core.model.MessageFieldMeta;
import com.slg.net.message.core.model.MessageMeta;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

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
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 标记读位置（用于回滚）
        in.markReaderIndex();
        
        // 1. 尝试读取 Length (VarInt)，处理粘包
        if (!isVarIntReadable(in)) {
            in.resetReaderIndex();
            return;  // VarInt 不完整，等待更多数据
        }
        int length = VarIntCodec.readVarInt(in);
        
        // 2. 检查消息体是否完整
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;  // 消息不完整，等待更多数据
        }
        
        // 3. 读取 MsgId (VarInt)
        int msgId = VarIntCodec.readVarInt(in);

        // 4. 查询类型和 Meta
        Class<?> clazz = registry.getClass(msgId);
        if (clazz == null) {
            LoggerUtil.error("未知的协议号: {}", msgId);
            throw new IllegalStateException("未知的协议号: " + msgId);
        }
        
        MessageMeta meta = registry.getMeta(clazz);
        if (meta == null) {
            LoggerUtil.error("未找到 Meta: {}", clazz.getName());
            throw new IllegalStateException("未找到 Meta: " + clazz.getName());
        }
        
        // 5. 验证可实例化
        if (!meta.isInstantiable()) {
            LoggerUtil.error("无法实例化抽象类/接口: {}", clazz.getName());
            throw new IllegalStateException("无法实例化抽象类/接口: " + clazz.getName());
        }
        
        try {
            // 6. 创建对象实例
            Object instance = meta.getConstructor().newInstance();
            
            // 7. 按字典序反序列化字段
            for (MessageFieldMeta field : meta.getFields()) {
                Object value = MessageCodec.readValue(in);
                field.getSetter().invoke(instance, value);
            }
            
            // 8. 输出消息对象
            out.add(instance);
        } catch (Throwable e) {
            LoggerUtil.error("消息解码失败，协议号: {}, 类型: {}", msgId, clazz.getName(), e);
            throw new IllegalStateException("消息解码失败: " + clazz.getName(), e);
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
        
        // VarInt 最多 5 字节（32位整数），逐字节检查
        int readerIndex = buf.readerIndex();
        for (int i = 0; i < Math.min(5, readableBytes); i++) {
            byte b = buf.getByte(readerIndex + i);
            // 最高位为 0 表示这是最后一个字节
            if ((b & 0x80) == 0) {
                return true;
            }
        }
        
        // 已经读取了 5 个字节但还没结束，或者不足 5 字节但未结束
        // 前者：数据完整（5字节是最大长度）
        // 后者：数据不完整
        return readableBytes >= 5;
    }
}

