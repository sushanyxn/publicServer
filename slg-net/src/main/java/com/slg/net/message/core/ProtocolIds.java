package com.slg.net.message.core;

/**
 * 协议号常量定义
 * 定义所有基础类型的协议号（0-50）
 * 用户自定义消息从 1000 开始
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class ProtocolIds {
    
    // ==================== 特殊类型 ====================
    /**
     * null 值的协议号
     */
    public static final int NULL = 0;
    
    // ==================== 基础类型（1-13） ====================
    /**
     * byte/Byte 类型协议号
     */
    public static final int BYTE = 1;
    
    /**
     * short/Short 类型协议号
     */
    public static final int SHORT = 2;
    
    /**
     * int/Integer 类型协议号
     */
    public static final int INT = 3;
    
    /**
     * long/Long 类型协议号
     */
    public static final int LONG = 4;
    
    /**
     * float/Float 类型协议号
     */
    public static final int FLOAT = 5;
    
    /**
     * double/Double 类型协议号
     */
    public static final int DOUBLE = 6;
    
    /**
     * boolean/Boolean 类型协议号
     */
    public static final int BOOLEAN = 7;
    
    /**
     * String 类型协议号
     */
    public static final int STRING = 8;
    
    /**
     * byte[] 类型协议号
     */
    public static final int BYTE_ARRAY = 9;
    
    /**
     * List 类型协议号
     */
    public static final int LIST = 10;
    
    /**
     * Set 类型协议号
     */
    public static final int SET = 11;
    
    /**
     * Map 类型协议号
     */
    public static final int MAP = 12;
    
    /**
     * Enum 类型协议号（所有枚举共用）
     */
    public static final int ENUM = 13;
    
    /**
     * Array 类型协议号（对象数组）
     */
    public static final int ARRAY = 14;
    
    // ==================== 用户消息起始 ID ====================
    /**
     * 用户自定义消息的起始协议号
     * message.yml 中配置的消息协议号应从此值开始
     * 100-999: 服务器内部消息
     * 1000+: 客户端交互消息
     */
    public static final int USER_MESSAGE_START = 100;
    
    /**
     * 私有构造函数，防止实例化
     */
    private ProtocolIds() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}

