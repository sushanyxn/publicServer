package com.slg.net.message.core.codec;

import io.netty.buffer.ByteBuf;

/**
 * VarInt 编解码器
 * 使用 Protobuf 风格的变长编码，压缩小整数的存储空间
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class VarIntCodec {
    
    /**
     * 写入 VarInt（32位整数）
     * 
     * @param buf 目标缓冲区
     * @param value 要写入的整数值
     */
    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value & 0x7F);
    }
    
    /**
     * 读取 VarInt（32位整数）
     * 
     * @param buf 源缓冲区
     * @return 读取的整数值
     */
    public static int readVarInt(ByteBuf buf) {
        int result = 0;
        int shift = 0;
        byte b;
        
        do {
            if (shift >= 32) {
                throw new IllegalStateException("VarInt 过大");
            }
            b = buf.readByte();
            result |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        
        return result;
    }
    
    /**
     * 写入 VarLong（64位长整数）
     * 
     * @param buf 目标缓冲区
     * @param value 要写入的长整数值
     */
    public static void writeVarLong(ByteBuf buf, long value) {
        while ((value & ~0x7FL) != 0) {
            buf.writeByte((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        buf.writeByte((int) (value & 0x7F));
    }
    
    /**
     * 读取 VarLong（64位长整数）
     * 
     * @param buf 源缓冲区
     * @return 读取的长整数值
     */
    public static long readVarLong(ByteBuf buf) {
        long result = 0;
        int shift = 0;
        byte b;
        
        do {
            if (shift >= 64) {
                throw new IllegalStateException("VarLong 过大");
            }
            b = buf.readByte();
            result |= (long) (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        
        return result;
    }
    
    /**
     * 计算 VarInt 所需的字节数
     * 
     * @param value 整数值
     * @return 所需字节数
     */
    public static int varIntSize(int value) {
        if ((value & (~0 << 7)) == 0) return 1;
        if ((value & (~0 << 14)) == 0) return 2;
        if ((value & (~0 << 21)) == 0) return 3;
        if ((value & (~0 << 28)) == 0) return 4;
        return 5;
    }
    
    /**
     * 计算 VarLong 所需的字节数
     * 
     * @param value 长整数值
     * @return 所需字节数
     */
    public static int varLongSize(long value) {
        if ((value & (~0L << 7)) == 0) return 1;
        if ((value & (~0L << 14)) == 0) return 2;
        if ((value & (~0L << 21)) == 0) return 3;
        if ((value & (~0L << 28)) == 0) return 4;
        if ((value & (~0L << 35)) == 0) return 5;
        if ((value & (~0L << 42)) == 0) return 6;
        if ((value & (~0L << 49)) == 0) return 7;
        if ((value & (~0L << 56)) == 0) return 8;
        if ((value & (~0L << 63)) == 0) return 9;
        return 10;
    }
    
    // ==================== ZigZag 编码（用于有符号整数） ====================
    
    /**
     * ZigZag 编码（32位）
     * 将有符号整数转换为无符号整数，使得绝对值小的负数也能高效编码
     * 
     * 映射规则：
     * 0 → 0, -1 → 1, 1 → 2, -2 → 3, 2 → 4, -3 → 5, ...
     * 
     * @param value 有符号整数
     * @return ZigZag 编码后的无符号整数
     */
    public static int encodeZigZag32(int value) {
        // (value << 1) ^ (value >> 31)
        // 正数：左移1位，异或0，结果 = value * 2
        // 负数：左移1位，异或-1（全1），结果 = -value * 2 - 1
        return (value << 1) ^ (value >> 31);
    }
    
    /**
     * ZigZag 解码（32位）
     * 将 ZigZag 编码的无符号整数还原为有符号整数
     * 
     * @param zigzag ZigZag 编码的无符号整数
     * @return 原始有符号整数
     */
    public static int decodeZigZag32(int zigzag) {
        // (zigzag >>> 1) ^ -(zigzag & 1)
        // 偶数：右移1位，异或0，结果 = zigzag / 2
        // 奇数：右移1位，异或-1（全1），结果 = -(zigzag + 1) / 2
        return (zigzag >>> 1) ^ -(zigzag & 1);
    }
    
    /**
     * 写入有符号 VarInt（使用 ZigZag 编码）
     * 适用于可能为负数的整数，能高效编码小的负数
     * 
     * @param buf 目标缓冲区
     * @param value 有符号整数
     */
    public static void writeSignedVarInt(ByteBuf buf, int value) {
        writeVarInt(buf, encodeZigZag32(value));
    }
    
    /**
     * 读取有符号 VarInt（使用 ZigZag 解码）
     * 
     * @param buf 源缓冲区
     * @return 有符号整数
     */
    public static int readSignedVarInt(ByteBuf buf) {
        return decodeZigZag32(readVarInt(buf));
    }
    
    /**
     * ZigZag 编码（64位）
     * 
     * @param value 有符号长整数
     * @return ZigZag 编码后的无符号长整数
     */
    public static long encodeZigZag64(long value) {
        return (value << 1) ^ (value >> 63);
    }
    
    /**
     * ZigZag 解码（64位）
     * 
     * @param zigzag ZigZag 编码的无符号长整数
     * @return 原始有符号长整数
     */
    public static long decodeZigZag64(long zigzag) {
        return (zigzag >>> 1) ^ -(zigzag & 1);
    }
    
    /**
     * 写入有符号 VarLong（使用 ZigZag 编码）
     * 
     * @param buf 目标缓冲区
     * @param value 有符号长整数
     */
    public static void writeSignedVarLong(ByteBuf buf, long value) {
        writeVarLong(buf, encodeZigZag64(value));
    }
    
    /**
     * 读取有符号 VarLong（使用 ZigZag 解码）
     * 
     * @param buf 源缓冲区
     * @return 有符号长整数
     */
    public static long readSignedVarLong(ByteBuf buf) {
        return decodeZigZag64(readVarLong(buf));
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private VarIntCodec() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }
}

