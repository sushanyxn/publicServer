package com.slg.table.util;

import com.slg.common.log.LoggerUtil;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置表类型转换器
 * 支持基本类型、数组、List、Map 以及自定义对象的解析
 * 
 * @author yangxunan
 * @date 2026/01/14
 */
public class TableTypeConverter {
    
    /**
     * 将字符串值转换为指定类型并设置到字段
     * 
     * @param field 目标字段
     * @param obj 目标对象
     * @param value 字符串值
     * @throws IllegalAccessException 反射访问异常
     */
    public static void convertAndSet(Field field, Object obj, String value) throws IllegalAccessException {
        // 空值不设置（使用字段默认值）
        if (value == null || value.isEmpty()) {
            return;
        }
        
        Class<?> fieldType = field.getType();
        Object convertedValue = convertValue(value, fieldType, field.getGenericType());
        field.set(obj, convertedValue);
    }
    
    /**
     * 将字符串值转换为指定类型
     * 
     * @param value 字符串值
     * @param targetType 目标类型
     * @param genericType 泛型类型信息
     * @return 转换后的对象
     */
    private static Object convertValue(String value, Class<?> targetType, Type genericType) {
        try {
            // 1. 基本类型和包装类型
            if (isPrimitiveOrWrapper(targetType)) {
                return convertPrimitive(value, targetType);
            }
            
            // 2. String 类型
            if (targetType == String.class) {
                return value;
            }
            
            // 3. 枚举类型
            if (targetType.isEnum()) {
                return convertEnum(value, targetType);
            }
            
            // 4. 数组类型
            if (targetType.isArray()) {
                return convertArray(value, targetType);
            }
            
            // 5. List 类型
            if (List.class.isAssignableFrom(targetType)) {
                return convertList(value, genericType);
            }
            
            // 6. Map 类型
            if (Map.class.isAssignableFrom(targetType)) {
                return convertMap(value, genericType);
            }
            
            // 7. 自定义对象类型（预留接口）
            return convertCustomObject(value, targetType);
            
        } catch (Exception e) {
            String errorMsg = String.format("类型转换失败：值='%s', 目标类型=%s", 
                value, targetType.getSimpleName());
            LoggerUtil.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }
    
    /**
     * 判断是否为基本类型或其包装类
     */
    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() 
            || type == Integer.class 
            || type == Long.class 
            || type == Double.class 
            || type == Float.class 
            || type == Boolean.class 
            || type == Byte.class 
            || type == Short.class 
            || type == Character.class;
    }
    
    /**
     * 转换基本类型
     */
    private static Object convertPrimitive(String value, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        }
        if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        }
        if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        }
        if (type == char.class || type == Character.class) {
            return value.isEmpty() ? '\0' : value.charAt(0);
        }
        
        throw new IllegalArgumentException("不支持的基本类型: " + type.getName());
    }
    
    /**
     * 转换枚举类型
     * 
     * @param value 字符串值（枚举常量名称）
     * @param enumType 枚举类型
     * @return 枚举常量
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertEnum(String value, Class<?> enumType) {
        try {
            return Enum.valueOf((Class<Enum>) enumType, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("无效的枚举值: %s，类型: %s", value, enumType.getSimpleName()), e);
        }
    }
    
    /**
     * 转换数组类型
     * 格式：元素1,元素2,元素3
     */
    private static Object convertArray(String value, Class<?> arrayType) {
        if (value.isEmpty()) {
            return Array.newInstance(arrayType.getComponentType(), 0);
        }
        
        Class<?> componentType = arrayType.getComponentType();
        String[] elements = splitElements(value);
        
        Object array = Array.newInstance(componentType, elements.length);
        for (int i = 0; i < elements.length; i++) {
            Object element = convertValue(elements[i], componentType, componentType);
            Array.set(array, i, element);
        }
        
        return array;
    }
    
    /**
     * 转换 List 类型
     * 格式：元素1,元素2,元素3
     */
    private static Object convertList(String value, Type genericType) {
        List<Object> list = new ArrayList<>();
        
        if (value.isEmpty()) {
            return list;
        }
        
        // 获取 List 的元素类型
        Class<?> elementType = getGenericType(genericType, 0);
        if (elementType == null) {
            elementType = String.class; // 默认为 String
        }
        
        String[] elements = splitElements(value);
        for (String element : elements) {
            Object convertedElement = convertValue(element, elementType, elementType);
            list.add(convertedElement);
        }
        
        return list;
    }
    
    /**
     * 转换 Map 类型
     * 格式：key1:value1,key2:value2,key3:value3
     * 注意：CSV 文件中包含逗号的字段需要用双引号包裹
     */
    private static Object convertMap(String value, Type genericType) {
        Map<Object, Object> map = new HashMap<>();
        
        if (value.isEmpty()) {
            return map;
        }
        
        // 获取 Map 的 Key 和 Value 类型
        Class<?> keyType = getGenericType(genericType, 0);
        Class<?> valueType = getGenericType(genericType, 1);
        
        if (keyType == null) keyType = String.class;
        if (valueType == null) valueType = String.class;
        
        // 使用逗号分隔键值对（CSV 文件中此字段需要用引号包裹）
        String[] pairs = value.split(",");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) {
                continue;
            }
            
            String[] kv = pair.split(":", 2); // 限制分割为2部分
            if (kv.length != 2) {
                LoggerUtil.warn("Map 格式错误，跳过该项: {}", pair);
                continue;
            }
            
            Object key = convertValue(kv[0].trim(), keyType, keyType);
            Object val = convertValue(kv[1].trim(), valueType, valueType);
            map.put(key, val);
        }
        
        return map;
    }
    
    /**
     * 分割元素（支持嵌套）
     * 处理逗号分隔，但要注意嵌套结构中的逗号
     * 支持 {} 花括号嵌套，花括号内的逗号不作为分隔符
     */
    private static String[] splitElements(String value) {
        List<String> elements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0; // 花括号深度
        
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            
            // 处理花括号深度
            if (c == '{') {
                depth++;
                current.append(c);
            } else if (c == '}') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                // 只有在深度为 0 时，逗号才是分隔符
                if (current.length() > 0) {
                    elements.add(current.toString().trim());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        // 添加最后一个元素
        if (current.length() > 0) {
            elements.add(current.toString().trim());
        }
        
        return elements.toArray(new String[0]);
    }
    
    /**
     * 获取泛型参数类型
     * 
     * @param genericType 泛型类型
     * @param index 泛型参数索引（0表示第一个泛型参数）
     * @return 泛型参数的 Class，如果无法获取则返回 null
     */
    private static Class<?> getGenericType(Type genericType, int index) {
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }
        
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        
        if (index >= actualTypeArguments.length) {
            return null;
        }
        
        Type targetType = actualTypeArguments[index];
        if (targetType instanceof Class) {
            return (Class<?>) targetType;
        }
        
        // 处理泛型通配符等复杂情况
        if (targetType instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) targetType).getRawType();
        }
        
        return null;
    }
    
    /**
     * 转换自定义对象（TableBean）
     * 
     * <p>支持的格式：类名|{参数1_参数2_参数3}
     * <p>示例：CurrencyConsume|{1_100} 表示创建 CurrencyConsume 对象，参数为 1 和 100
     * 
     * <p>解析规则：
     * <ol>
     *   <li>按 | 分割，左边是类名（简单类名），右边是参数</li>
     *   <li>参数使用 {} 包裹，内部用 _ 分隔</li>
     *   <li>按字段声明顺序依次赋值</li>
     *   <li>类必须使用 @TableBean 注解标记</li>
     * </ol>
     * 
     * @param value 字符串值，格式：类名|{参数1_参数2_参数3}
     * @param targetType 目标类型
     * @return 转换后的对象
     */
    private static Object convertCustomObject(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        try {
            // 获取 TableBeanManager 单例
            com.slg.table.manager.TableBeanManager tableBeanManager = 
                com.slg.table.manager.TableBeanManager.getInstance();
            
            if (tableBeanManager == null) {
                LoggerUtil.error("TableBeanManager 未初始化，无法转换自定义对象");
                return null;
            }
            
            return parseTableBean(value, targetType, tableBeanManager);
            
        } catch (Exception e) {
            String errorMsg = String.format("自定义对象转换失败: 值='%s', 目标类型=%s", 
                value, targetType.getSimpleName());
            LoggerUtil.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }
    
    /**
     * 解析 TableBean 对象
     * 
     * @param value 字符串值，格式：类名|{参数1_参数2_参数3}
     * @param targetType 目标类型
     * @param tableBeanManager TableBean 管理器
     * @return 解析后的对象
     */
    private static Object parseTableBean(String value, Class<?> targetType, 
            com.slg.table.manager.TableBeanManager tableBeanManager) throws Exception {
        
        // 1. 解析格式：类名|{参数1_参数2_参数3}
        int separatorIndex = value.indexOf('|');
        if (separatorIndex == -1) {
            throw new IllegalArgumentException(
                String.format("TableBean 格式错误，缺少 | 分隔符: %s", value));
        }
        
        String className = value.substring(0, separatorIndex).trim();
        String paramsStr = value.substring(separatorIndex + 1).trim();
        
        // 2. 验证参数格式：{参数1_参数2_参数3}
        if (!paramsStr.startsWith("{") || !paramsStr.endsWith("}")) {
            throw new IllegalArgumentException(
                String.format("TableBean 参数格式错误，必须使用 {} 包裹: %s", paramsStr));
        }
        
        // 3. 提取参数
        String params = paramsStr.substring(1, paramsStr.length() - 1).trim();
        String[] paramValues = params.isEmpty() ? new String[0] : params.split("_");
        
        // 4. 获取 TableBean 类
        Class<?> beanClass = tableBeanManager.getTableBean(className);
        if (beanClass == null) {
            throw new IllegalArgumentException(
                String.format("未找到 TableBean 类: %s，请确保该类已使用 @TableBean 注解", className));
        }
        
        // 5. 验证类型匹配
        if (!targetType.isAssignableFrom(beanClass)) {
            throw new IllegalArgumentException(
                String.format("类型不匹配: 期望 %s，实际 %s", 
                    targetType.getName(), beanClass.getName()));
        }
        
        // 6. 创建对象实例
        Object instance = beanClass.getDeclaredConstructor().newInstance();
        
        // 7. 获取所有字段并按声明顺序赋值
        Field[] fields = beanClass.getDeclaredFields();
        
        if (paramValues.length > fields.length) {
            LoggerUtil.warn("TableBean {} 参数数量({})超过字段数量({}), 多余的参数将被忽略",
                className, paramValues.length, fields.length);
        }
        
        // 8. 依次设置字段值
        for (int i = 0; i < Math.min(paramValues.length, fields.length); i++) {
            Field field = fields[i];
            String paramValue = paramValues[i].trim();
            
            field.setAccessible(true);
            
            // 类型转换并设置值
            Class<?> fieldType = field.getType();
            Object convertedValue = convertValue(paramValue, fieldType, field.getGenericType());
            field.set(instance, convertedValue);
        }
        
        return instance;
    }
}

