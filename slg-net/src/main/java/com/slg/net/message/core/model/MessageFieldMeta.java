package com.slg.net.message.core.model;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;

/**
 * 字段元信息
 * 保存消息类中单个字段的元数据，包括类型信息和访问器
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class MessageFieldMeta {
    
    /**
     * 字段名
     */
    private final String name;
    
    /**
     * 字段类型
     */
    private final Class<?> fieldType;
    
    /**
     * 字段的泛型类型（完整类型信息，包含泛型参数）
     */
    private final Type genericType;
    
    /**
     * 字段的 getter MethodHandle
     */
    private final MethodHandle getter;
    
    /**
     * 字段的 setter MethodHandle
     */
    private final MethodHandle setter;
    
    /**
     * 字段在字典序中的索引位置
     */
    private final int index;
    
    /**
     * 构造函数
     * 
     * @param name 字段名
     * @param fieldType 字段类型
     * @param genericType 泛型类型
     * @param getter getter 方法句柄
     * @param setter setter 方法句柄
     * @param index 字段索引
     */
    public MessageFieldMeta(String name, Class<?> fieldType, Type genericType,
                           MethodHandle getter, MethodHandle setter, int index) {
        this.name = name;
        this.fieldType = fieldType;
        this.genericType = genericType;
        this.getter = getter;
        this.setter = setter;
        this.index = index;
    }
    
    /**
     * 获取字段名
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取字段类型
     */
    public Class<?> getFieldType() {
        return fieldType;
    }
    
    /**
     * 获取泛型类型
     */
    public Type getGenericType() {
        return genericType;
    }
    
    /**
     * 获取 getter 方法句柄
     */
    public MethodHandle getGetter() {
        return getter;
    }
    
    /**
     * 获取 setter 方法句柄
     */
    public MethodHandle getSetter() {
        return setter;
    }
    
    /**
     * 获取字段索引
     */
    public int getIndex() {
        return index;
    }
    
    @Override
    public String toString() {
        return "MessageFieldMeta{" +
                "name='" + name + '\'' +
                ", fieldType=" + fieldType.getSimpleName() +
                ", index=" + index +
                '}';
    }
}

