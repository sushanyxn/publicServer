package com.slg.net.message.core.model;

import com.slg.common.log.LoggerUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 消息元信息工厂
 * 负责为消息类生成 MessageMeta 对象
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class MessageMetaFactory {
    
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    
    /**
     * 为指定类生成 MessageMeta
     * 
     * @param protocolId 协议号
     * @param clazz 消息类
     * @return MessageMeta 对象
     */
    public MessageMeta createMeta(int protocolId, Class<?> clazz) {

        // 判断是否可实例化
        boolean instantiable = !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isInterface();
        
        // 获取无参构造函数
        Constructor<?> constructor = null;
        if (instantiable) {
            try {
                constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                LoggerUtil.error("类 {} 没有无参构造函数", clazz.getName());
                throw new IllegalStateException("消息类必须有无参构造函数: " + clazz.getName(), e);
            }
        }
        
        // 获取所有字段（包括父类）
        List<Field> allFields = getAllFields(clazz);
        
        // 过滤：排除 static、transient 字段
        List<Field> serializableFields = allFields.stream()
            .filter(f -> !Modifier.isStatic(f.getModifiers()))
            .filter(f -> !Modifier.isTransient(f.getModifiers()))
            .toList();
        
        // 按字段名字典序排序
        List<Field> sortedFields = serializableFields.stream()
            .sorted(Comparator.comparing(Field::getName))
            .toList();
        
        // 为每个字段创建 MessageFieldMeta
        List<MessageFieldMeta> fieldMetas = new ArrayList<>();
        for (int i = 0; i < sortedFields.size(); i++) {
            Field field = sortedFields.get(i);
            MessageFieldMeta fieldMeta = createFieldMeta(field, i);
            fieldMetas.add(fieldMeta);
        }

        return new MessageMeta(protocolId, clazz, instantiable, fieldMetas, constructor);
    }
    
    /**
     * 为单个字段创建 MessageFieldMeta
     * 
     * @param field 字段
     * @param index 字段索引
     * @return MessageFieldMeta 对象
     */
    private MessageFieldMeta createFieldMeta(Field field, int index) {
        field.setAccessible(true);
        
        String name = field.getName();
        Class<?> fieldType = field.getType();
        
        try {
            // 创建 getter 和 setter 的 MethodHandle
            MethodHandle getter = lookup.unreflectGetter(field);
            MethodHandle setter = lookup.unreflectSetter(field);
            
            return new MessageFieldMeta(name, fieldType, field.getGenericType(), 
                                       getter, setter, index);
        } catch (IllegalAccessException e) {
            LoggerUtil.error("无法创建字段 {} 的 MethodHandle", name, e);
            throw new IllegalStateException("无法访问字段: " + name, e);
        }
    }
    
    /**
     * 获取类的所有字段（包括父类，排除 Object 类）
     * 
     * @param clazz 类
     * @return 所有字段列表
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null && current != Object.class) {
            Field[] declaredFields = current.getDeclaredFields();
            for (Field field : declaredFields) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        
        return fields;
    }
}

