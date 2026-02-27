package com.slg.net.message.core.codec;

import com.slg.net.message.core.ProtocolIds;
import com.slg.net.message.core.manager.MessageRegistry;
import com.slg.net.message.core.model.MessageFieldMeta;
import com.slg.net.message.core.model.MessageMeta;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 消息编解码工具类
 * 提供所有类型的编解码方法
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class MessageCodec {
    
    private static final MessageRegistry registry = MessageRegistry.getInstance();
    
    /**
     * 写入任意类型的值（带类型协议号）
     * 
     * @param buf 目标缓冲区
     * @param value 要写入的值
     */
    public static void writeValue(ByteBuf buf, Object value) throws Throwable {
        // null 处理
        if (value == null) {
            VarIntCodec.writeVarInt(buf, ProtocolIds.NULL);
            return;
        }
        
        // 获取实际类型
        Class<?> actualType = value.getClass();
        
        // 特殊处理数组类型（除了 byte[] 外）
        if (actualType.isArray() && actualType != byte[].class) {
            VarIntCodec.writeVarInt(buf, ProtocolIds.ARRAY);
            writeArray(buf, value);
            return;
        }
        
        Integer protocolId = registry.getProtocolId(actualType);
        
        if (protocolId == null) {
            throw new IllegalStateException("未注册的类型: " + actualType.getName());
        }
        
        // 写入协议号
        VarIntCodec.writeVarInt(buf, protocolId);
        
        // 根据协议号选择编码方式
        switch (protocolId) {
            case ProtocolIds.BYTE -> writeByte(buf, ((Number) value).byteValue());
            case ProtocolIds.SHORT -> writeShort(buf, ((Number) value).shortValue());
            case ProtocolIds.INT -> writeInt(buf, ((Number) value).intValue());
            case ProtocolIds.LONG -> writeLong(buf, ((Number) value).longValue());
            case ProtocolIds.FLOAT -> writeFloat(buf, ((Number) value).floatValue());
            case ProtocolIds.DOUBLE -> writeDouble(buf, ((Number) value).doubleValue());
            case ProtocolIds.BOOLEAN -> writeBoolean(buf, (boolean) value);
            case ProtocolIds.STRING -> writeString(buf, (String) value);
            case ProtocolIds.BYTE_ARRAY -> writeByteArray(buf, (byte[]) value);
            case ProtocolIds.LIST -> writeList(buf, (List<?>) value);
            case ProtocolIds.SET -> writeSet(buf, (Set<?>) value);
            case ProtocolIds.MAP -> writeMap(buf, (Map<?, ?>) value);
            case ProtocolIds.ENUM -> writeEnum(buf, (Enum<?>) value);
            default -> writeObject(buf, value);  // 自定义对象
        }
    }
    
    /**
     * 读取任意类型的值（根据类型协议号）
     * 
     * @param buf 源缓冲区
     * @return 读取的值
     */
    public static Object readValue(ByteBuf buf) throws Throwable {
        // 读取协议号
        int protocolId = VarIntCodec.readVarInt(buf);
        
        // null 处理
        if (protocolId == ProtocolIds.NULL) {
            return null;
        }
        
        // 根据协议号选择解码方式
        return switch (protocolId) {
            case ProtocolIds.BYTE -> readByte(buf);
            case ProtocolIds.SHORT -> readShort(buf);
            case ProtocolIds.INT -> readInt(buf);
            case ProtocolIds.LONG -> readLong(buf);
            case ProtocolIds.FLOAT -> readFloat(buf);
            case ProtocolIds.DOUBLE -> readDouble(buf);
            case ProtocolIds.BOOLEAN -> readBoolean(buf);
            case ProtocolIds.STRING -> readString(buf);
            case ProtocolIds.BYTE_ARRAY -> readByteArray(buf);
            case ProtocolIds.LIST -> readList(buf);
            case ProtocolIds.SET -> readSet(buf);
            case ProtocolIds.MAP -> readMap(buf);
            case ProtocolIds.ENUM -> readEnum(buf, protocolId);
            case ProtocolIds.ARRAY -> readArray(buf);
            default -> readObject(buf, protocolId);  // 自定义对象
        };
    }
    
    // ==================== 基础类型编解码 ====================
    
    private static void writeByte(ByteBuf buf, byte value) {
        buf.writeByte(value);
    }
    
    private static byte readByte(ByteBuf buf) {
        return buf.readByte();
    }
    
    private static void writeShort(ByteBuf buf, short value) {
        buf.writeShort(value);
    }
    
    private static short readShort(ByteBuf buf) {
        return buf.readShort();
    }
    
    private static void writeInt(ByteBuf buf, int value) {
        // 使用 ZigZag 编码，高效处理负数
        VarIntCodec.writeSignedVarInt(buf, value);
    }
    
    private static int readInt(ByteBuf buf) {
        // 使用 ZigZag 解码
        return VarIntCodec.readSignedVarInt(buf);
    }
    
    private static void writeLong(ByteBuf buf, long value) {
        // 使用 ZigZag 编码，高效处理负数
        VarIntCodec.writeSignedVarLong(buf, value);
    }
    
    private static long readLong(ByteBuf buf) {
        // 使用 ZigZag 解码
        return VarIntCodec.readSignedVarLong(buf);
    }
    
    private static void writeFloat(ByteBuf buf, float value) {
        buf.writeFloat(value);
    }
    
    private static float readFloat(ByteBuf buf) {
        return buf.readFloat();
    }
    
    private static void writeDouble(ByteBuf buf, double value) {
        buf.writeDouble(value);
    }
    
    private static double readDouble(ByteBuf buf) {
        return buf.readDouble();
    }
    
    private static void writeBoolean(ByteBuf buf, boolean value) {
        buf.writeBoolean(value);
    }
    
    private static boolean readBoolean(ByteBuf buf) {
        return buf.readBoolean();
    }
    
    // ==================== String 编解码 ====================
    
    private static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        VarIntCodec.writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }
    
    private static String readString(ByteBuf buf) {
        int length = VarIntCodec.readVarInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    // ==================== byte[] 编解码 ====================
    
    private static void writeByteArray(ByteBuf buf, byte[] value) {
        VarIntCodec.writeVarInt(buf, value.length);
        buf.writeBytes(value);
    }
    
    private static byte[] readByteArray(ByteBuf buf) {
        int length = VarIntCodec.readVarInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }
    
    // ==================== Enum 编解码 ====================
    
    private static void writeEnum(ByteBuf buf, Enum<?> value) {
        writeString(buf, value.name());
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> readEnum(ByteBuf buf, int protocolId) {
        String name = readString(buf);
        Class<?> enumType = registry.getClass(protocolId);
        if (enumType == null || !enumType.isEnum()) {
            throw new IllegalStateException("协议号 " + protocolId + " 不是枚举类型");
        }
        return Enum.valueOf((Class<? extends Enum>) enumType, name);
    }
    
    // ==================== List 编解码 ====================
    
    private static void writeList(ByteBuf buf, List<?> list) throws Throwable {
        VarIntCodec.writeVarInt(buf, list.size());
        for (Object element : list) {
            writeValue(buf, element);
        }
    }
    
    private static List<Object> readList(ByteBuf buf) throws Throwable {
        int size = VarIntCodec.readVarInt(buf);
        List<Object> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readValue(buf));
        }
        return list;
    }
    
    // ==================== Set 编解码 ====================
    
    private static void writeSet(ByteBuf buf, Set<?> set) throws Throwable {
        VarIntCodec.writeVarInt(buf, set.size());
        for (Object element : set) {
            writeValue(buf, element);
        }
    }
    
    private static Set<Object> readSet(ByteBuf buf) throws Throwable {
        int size = VarIntCodec.readVarInt(buf);
        Set<Object> set = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            set.add(readValue(buf));
        }
        return set;
    }
    
    // ==================== Map 编解码 ====================
    
    private static void writeMap(ByteBuf buf, Map<?, ?> map) throws Throwable {
        VarIntCodec.writeVarInt(buf, map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writeValue(buf, entry.getKey());
            writeValue(buf, entry.getValue());
        }
    }
    
    private static Map<Object, Object> readMap(ByteBuf buf) throws Throwable {
        int size = VarIntCodec.readVarInt(buf);
        Map<Object, Object> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            Object key = readValue(buf);
            Object value = readValue(buf);
            map.put(key, value);
        }
        return map;
    }
    
    // ==================== Array 编解码 ====================
    
    private static void writeArray(ByteBuf buf, Object array) throws Throwable {
        int length = java.lang.reflect.Array.getLength(array);
        VarIntCodec.writeVarInt(buf, length);
        for (int i = 0; i < length; i++) {
            Object element = java.lang.reflect.Array.get(array, i);
            writeValue(buf, element);
        }
    }
    
    private static Object[] readArray(ByteBuf buf) throws Throwable {
        int length = VarIntCodec.readVarInt(buf);
        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
            array[i] = readValue(buf);
        }
        return array;
    }
    
    // ==================== 自定义对象编解码 ====================
    
    private static void writeObject(ByteBuf buf, Object obj) throws Throwable {
        MessageMeta meta = registry.getMeta(obj.getClass());
        if (meta == null) {
            throw new IllegalStateException("未注册的类型: " + obj.getClass().getName());
        }
        
        // 按字典序序列化字段
        for (MessageFieldMeta field : meta.getFields()) {
            Object value = field.getGetter().invoke(obj);
            writeValue(buf, value);
        }
    }
    
    private static Object readObject(ByteBuf buf, int protocolId) throws Throwable {
        Class<?> clazz = registry.getClass(protocolId);
        if (clazz == null) {
            throw new IllegalStateException("未知的协议号: " + protocolId);
        }
        
        MessageMeta meta = registry.getMeta(clazz);
        if (meta == null) {
            throw new IllegalStateException("未找到 Meta: " + clazz.getName());
        }
        
        if (!meta.isInstantiable()) {
            throw new IllegalStateException("无法实例化类型: " + clazz.getName());
        }
        
        // 创建实例
        Object instance = meta.getConstructor().newInstance();
        
        // 按字典序反序列化字段
        for (MessageFieldMeta field : meta.getFields()) {
            Object value = readValue(buf);
            field.getSetter().invoke(instance, value);
        }
        
        return instance;
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private MessageCodec() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }
}

