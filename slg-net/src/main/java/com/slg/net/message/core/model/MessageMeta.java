package com.slg.net.message.core.model;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

/**
 * 消息元信息
 * 保存消息类的完整元数据，包括协议号、字段列表和构造函数
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class MessageMeta {
    
    /**
     * 协议号
     */
    private final int protocolId;
    
    /**
     * 消息类型
     */
    private final Class<?> messageType;
    
    /**
     * 是否可实例化（非抽象类、非接口）
     */
    private final boolean instantiable;
    
    /**
     * 字段列表（按字典序排列，不可变）
     */
    private final List<MessageFieldMeta> fields;
    
    /**
     * 无参构造函数（可能为 null，如果不可实例化）
     */
    private final Constructor<?> constructor;
    
    /**
     * 构造函数
     * 
     * @param protocolId 协议号
     * @param messageType 消息类型
     * @param instantiable 是否可实例化
     * @param fields 字段列表（按字典序）
     * @param constructor 无参构造函数
     */
    public MessageMeta(int protocolId, Class<?> messageType, boolean instantiable,
                      List<MessageFieldMeta> fields, Constructor<?> constructor) {
        this.protocolId = protocolId;
        this.messageType = messageType;
        this.instantiable = instantiable;
        this.fields = Collections.unmodifiableList(fields);
        this.constructor = constructor;
    }
    
    /**
     * 获取协议号
     */
    public int getProtocolId() {
        return protocolId;
    }
    
    /**
     * 获取消息类型
     */
    public Class<?> getMessageType() {
        return messageType;
    }
    
    /**
     * 是否可实例化
     */
    public boolean isInstantiable() {
        return instantiable;
    }
    
    /**
     * 获取字段列表（不可变）
     */
    public List<MessageFieldMeta> getFields() {
        return fields;
    }
    
    /**
     * 获取无参构造函数
     */
    public Constructor<?> getConstructor() {
        return constructor;
    }
    
    @Override
    public String toString() {
        return "MessageMeta{" +
                "protocolId=" + protocolId +
                ", messageType=" + messageType.getName() +
                ", instantiable=" + instantiable +
                ", fieldsCount=" + fields.size() +
                '}';
    }
}

