package com.slg.common.util;

import com.slg.common.log.LoggerUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean 工具类
 * 提供对象属性拷贝、反射操作等常用方法
 * 
 * @author yangxunan
 * @date 2025-12-23
 */
public class BeanUtil {

    /**
     * 浅拷贝对象属性（只拷贝非 null 值）
     * 要求源对象和目标对象的属性名称和类型一致
     *
     * @param source 源对象
     * @param target 目标对象
     * @param <T> 目标对象类型
     * @return 目标对象
     */
    public static <T> T copyProperties(Object source, T target) {
        return copyProperties(source, target, false);
    }

    /**
     * 拷贝对象属性
     * 要求源对象和目标对象的属性名称和类型一致
     *
     * @param source 源对象
     * @param target 目标对象
     * @param copyNull 是否拷贝 null 值
     * @param <T> 目标对象类型
     * @return 目标对象
     */
    public static <T> T copyProperties(Object source, T target, boolean copyNull) {
        if (source == null || target == null) {
            return target;
        }

        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();

        Field[] sourceFields = sourceClass.getDeclaredFields();

        for (Field sourceField : sourceFields) {
            try {
                // 跳过 static 和 final 字段
                if (java.lang.reflect.Modifier.isStatic(sourceField.getModifiers()) ||
                    java.lang.reflect.Modifier.isFinal(sourceField.getModifiers())) {
                    continue;
                }

                sourceField.setAccessible(true);
                Object value = sourceField.get(source);

                // 如果不拷贝 null 值，且值为 null，则跳过
                if (!copyNull && value == null) {
                    continue;
                }

                // 在目标对象中查找同名字段
                Field targetField;
                try {
                    targetField = targetClass.getDeclaredField(sourceField.getName());
                } catch (NoSuchFieldException e) {
                    // 目标对象没有该字段，跳过
                    continue;
                }

                // 检查类型是否一致
                if (!targetField.getType().equals(sourceField.getType())) {
                    continue;
                }

                targetField.setAccessible(true);
                targetField.set(target, value);

            } catch (Exception e) {
                LoggerUtil.error("属性拷贝失败: field={}", sourceField.getName(), e);
            }
        }

        return target;
    }

    /**
     * 创建对象并拷贝属性
     *
     * @param source 源对象
     * @param targetClass 目标类
     * @param <T> 目标对象类型
     * @return 新创建的目标对象
     */
    public static <T> T copyToNewInstance(Object source, Class<T> targetClass) {
        if (source == null || targetClass == null) {
            return null;
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            return copyProperties(source, target);
        } catch (Exception e) {
            LoggerUtil.error("创建对象并拷贝属性失败: {}", targetClass.getName(), e);
            return null;
        }
    }

    /**
     * 将对象转换为 Map
     * 键为属性名，值为属性值
     *
     * @param obj 对象
     * @return Map 集合
     */
    public static Map<String, Object> toMap(Object obj) {
        Map<String, Object> map = new HashMap<>();
        if (obj == null) {
            return map;
        }

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                // 跳过 static 字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(obj);
                map.put(field.getName(), value);
            } catch (Exception e) {
                LoggerUtil.error("对象转Map失败: field={}", field.getName(), e);
            }
        }

        return map;
    }

    /**
     * 从 Map 填充对象属性
     *
     * @param map Map 集合
     * @param obj 目标对象
     * @param <T> 目标对象类型
     * @return 目标对象
     */
    public static <T> T fromMap(Map<String, Object> map, T obj) {
        if (map == null || map.isEmpty() || obj == null) {
            return obj;
        }

        Class<?> clazz = obj.getClass();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);

                // 检查类型是否匹配
                if (field.getType().isAssignableFrom(value.getClass())) {
                    field.set(obj, value);
                }
            } catch (NoSuchFieldException e) {
                // 字段不存在，跳过
            } catch (Exception e) {
                LoggerUtil.error("从Map填充对象失败: field={}", fieldName, e);
            }
        }

        return obj;
    }

    /**
     * 获取对象字段的值
     *
     * @param obj 对象
     * @param fieldName 字段名
     * @return 字段值，获取失败返回 null
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }

        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            LoggerUtil.error("获取字段值失败: field={}", fieldName, e);
            return null;
        }
    }

    /**
     * 设置对象字段的值
     *
     * @param obj 对象
     * @param fieldName 字段名
     * @param value 字段值
     * @return 是否设置成功
     */
    public static boolean setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return false;
        }

        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (Exception e) {
            LoggerUtil.error("设置字段值失败: field={}", fieldName, e);
            return false;
        }
    }

    /**
     * 调用对象的方法
     *
     * @param obj 对象
     * @param methodName 方法名
     * @param args 方法参数
     * @return 方法返回值，调用失败返回 null
     */
    public static Object invokeMethod(Object obj, String methodName, Object... args) {
        if (obj == null || methodName == null || methodName.isEmpty()) {
            return null;
        }

        try {
            Class<?>[] parameterTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }

            Method method = obj.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (Exception e) {
            LoggerUtil.error("调用方法失败: method={}", methodName, e);
            return null;
        }
    }

    /**
     * 判断对象是否有指定字段
     *
     * @param obj 对象
     * @param fieldName 字段名
     * @return 是否存在
     */
    public static boolean hasField(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.isEmpty()) {
            return false;
        }

        try {
            obj.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * 判断对象是否有指定方法
     *
     * @param obj 对象
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 是否存在
     */
    public static boolean hasMethod(Object obj, String methodName, Class<?>... parameterTypes) {
        if (obj == null || methodName == null || methodName.isEmpty()) {
            return false;
        }

        try {
            obj.getClass().getDeclaredMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}



