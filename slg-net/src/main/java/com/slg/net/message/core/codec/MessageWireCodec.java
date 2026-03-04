package com.slg.net.message.core.codec;

import com.slg.net.message.core.manager.MessageRegistry;
import com.slg.net.message.core.model.MessageFieldMeta;
import com.slg.net.message.core.model.MessageMeta;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 消息线缆编解码器
 * 与 WebSocket 线缆格式完全一致：Length(VarInt) + MsgId(VarInt) + Body
 * 用于将消息对象编解码为 byte[]，适用于非 Netty 管道传输（如 Redis Stream）
 *
 * @author yangxunan
 * @date 2026/03/04
 */
public class MessageWireCodec {

    private static final MessageRegistry registry = MessageRegistry.getInstance();

    /**
     * 将消息对象编码为 byte[]（线缆格式：Length + MsgId + Body）
     *
     * @param message 消息对象（必须已通过 message.yml 注册协议号）
     * @return 编码后的字节数组
     * @throws IllegalStateException 类型未注册或字段序列化失败
     */
    public static byte[] encode(Object message) {
        Class<?> actualType = message.getClass();
        Integer msgId = registry.getProtocolId(actualType);
        if (msgId == null) {
            throw new IllegalStateException("未注册的消息类型: " + actualType.getName());
        }

        MessageMeta meta = registry.getMeta(actualType);
        if (meta == null) {
            throw new IllegalStateException("未找到 Meta: " + actualType.getName());
        }

        ByteBuf bodyBuf = Unpooled.buffer(64);
        try {
            for (MessageFieldMeta field : meta.getFields()) {
                Object value = field.getGetter().invoke(message);
                MessageCodec.writeValue(bodyBuf, value);
            }

            int bodyLength = bodyBuf.readableBytes();
            int msgIdLength = VarIntCodec.varIntSize(msgId);
            int totalLength = msgIdLength + bodyLength;

            ByteBuf out = Unpooled.buffer(VarIntCodec.varIntSize(totalLength) + totalLength);
            try {
                VarIntCodec.writeVarInt(out, totalLength);
                VarIntCodec.writeVarInt(out, msgId);
                out.writeBytes(bodyBuf);

                byte[] result = new byte[out.readableBytes()];
                out.readBytes(result);
                return result;
            } finally {
                out.release();
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("消息编码失败: " + actualType.getName(), e);
        } finally {
            bodyBuf.release();
        }
    }

    /**
     * 将 byte[] 解码为消息对象（线缆格式：Length + MsgId + Body）
     *
     * @param bytes 编码后的字节数组
     * @return 解码后的消息对象
     * @throws IllegalStateException 协议号未知、数据不完整或反序列化失败
     */
    public static Object decode(byte[] bytes) {
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        try {
            int length = VarIntCodec.readVarInt(buf);
            if (buf.readableBytes() < length) {
                throw new IllegalStateException(
                        "消息数据不完整，期望长度: " + length + "，实际可读: " + buf.readableBytes());
            }

            int msgId = VarIntCodec.readVarInt(buf);
            Class<?> clazz = registry.getClass(msgId);
            if (clazz == null) {
                throw new IllegalStateException("未知的协议号: " + msgId);
            }

            MessageMeta meta = registry.getMeta(clazz);
            if (meta == null || !meta.isInstantiable()) {
                throw new IllegalStateException("无法实例化类型，协议号: " + msgId);
            }

            Object instance = meta.getConstructor().newInstance();
            for (MessageFieldMeta field : meta.getFields()) {
                Object value = MessageCodec.readValue(buf);
                field.getSetter().invoke(instance, value);
            }
            return instance;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("消息解码失败", e);
        } finally {
            buf.release();
        }
    }

    private MessageWireCodec() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }
}
