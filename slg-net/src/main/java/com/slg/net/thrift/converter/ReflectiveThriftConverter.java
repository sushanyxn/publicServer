package com.slg.net.thrift.converter;

import com.slg.common.log.LoggerUtil;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.meta_data.FieldMetaData;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用反射转换器
 * 基于字段名自动映射 Thrift Struct 和 POJO，适用于字段名一致的协议
 * 启动时预构建 MethodHandle 访问器，运行时零反射
 *
 * @param <T> Thrift 消息类型
 * @param <R> POJO 类型
 * @author yangxunan
 * @date 2026/02/26
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReflectiveThriftConverter<T extends TBase<?, ?>, R> implements IThriftConverter<T, R> {

    private final Class<T> thriftType;
    private final Class<R> pojoType;
    private final Constructor<T> thriftConstructor;
    private final Constructor<R> pojoConstructor;
    private final List<FieldMapping> fieldMappings;

    public ReflectiveThriftConverter(Class<T> thriftType, Class<R> pojoType) {
        this.thriftType = thriftType;
        this.pojoType = pojoType;

        try {
            this.thriftConstructor = thriftType.getDeclaredConstructor();
            this.thriftConstructor.setAccessible(true);
            this.pojoConstructor = pojoType.getDeclaredConstructor();
            this.pojoConstructor.setAccessible(true);
            this.fieldMappings = buildFieldMappings();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "构建 ReflectiveThriftConverter 失败: thrift=" + thriftType.getName() +
                            ", pojo=" + pojoType.getName(), e);
        }

        if (fieldMappings.isEmpty()) {
            LoggerUtil.error("ReflectiveThriftConverter: thrift={} 和 pojo={} 之间无匹配字段",
                    thriftType.getSimpleName(), pojoType.getSimpleName());
        }
    }

    @Override
    public R fromThrift(T thriftMsg) {
        try {
            R pojo = pojoConstructor.newInstance();
            for (FieldMapping mapping : fieldMappings) {
                Object value = mapping.thriftGetter.invoke(thriftMsg);
                if (value != null) {
                    mapping.pojoSetter.invoke(pojo, convertValue(value, mapping.pojoFieldType));
                }
            }
            return pojo;
        } catch (Throwable e) {
            throw new RuntimeException("Thrift → POJO 转换失败: " + thriftType.getSimpleName(), e);
        }
    }

    @Override
    public T toThrift(R pojoMsg) {
        try {
            T thrift = thriftConstructor.newInstance();
            for (FieldMapping mapping : fieldMappings) {
                Object value = mapping.pojoGetter.invoke(pojoMsg);
                if (value != null) {
                    mapping.thriftSetter.invoke(thrift, convertValue(value, mapping.thriftFieldType));
                }
            }
            return thrift;
        } catch (Throwable e) {
            throw new RuntimeException("POJO → Thrift 转换失败: " + pojoType.getSimpleName(), e);
        }
    }

    @Override
    public Class<T> getThriftType() {
        return thriftType;
    }

    @Override
    public Class<R> getPojoType() {
        return pojoType;
    }

    /**
     * 构建字段映射列表
     * 遍历 Thrift 类的 metaDataMap 获取字段信息，与 POJO 字段按名称匹配
     */
    private List<FieldMapping> buildFieldMappings() throws Exception {
        List<FieldMapping> mappings = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Map<? extends TFieldIdEnum, FieldMetaData> metaDataMap = getThriftMetaDataMap();
        if (metaDataMap == null || metaDataMap.isEmpty()) {
            return buildFieldMappingsByReflection(lookup);
        }

        for (Map.Entry<? extends TFieldIdEnum, FieldMetaData> entry : metaDataMap.entrySet()) {
            String thriftFieldName = entry.getKey().getFieldName();
            String pojoFieldName = toCamelCase(thriftFieldName);

            Field pojoField = findField(pojoType, pojoFieldName);
            if (pojoField == null) {
                pojoField = findField(pojoType, thriftFieldName);
            }
            if (pojoField == null) {
                LoggerUtil.debug("Thrift 字段 {}.{} 在 POJO {} 中无匹配字段，跳过",
                        thriftType.getSimpleName(), thriftFieldName, pojoType.getSimpleName());
                continue;
            }

            FieldMapping mapping = createFieldMapping(lookup, thriftFieldName, pojoField);
            if (mapping != null) {
                mappings.add(mapping);
            }
        }

        return mappings;
    }

    /**
     * 退化方案：当 Thrift metaDataMap 不可用时，通过 POJO 字段反射匹配
     */
    private List<FieldMapping> buildFieldMappingsByReflection(MethodHandles.Lookup lookup) throws Exception {
        List<FieldMapping> mappings = new ArrayList<>();

        for (Field pojoField : pojoType.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(pojoField.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(pojoField.getModifiers())) {
                continue;
            }

            String fieldName = pojoField.getName();
            FieldMapping mapping = createFieldMapping(lookup, fieldName, pojoField);
            if (mapping != null) {
                mappings.add(mapping);
            }
        }

        return mappings;
    }

    /**
     * 为单个字段创建映射
     */
    private FieldMapping createFieldMapping(MethodHandles.Lookup lookup, String thriftFieldName, Field pojoField)
            throws Exception {
        String capitalizedThrift = capitalize(thriftFieldName);
        String capitalizedPojo = capitalize(pojoField.getName());

        Method thriftGetter = findGetter(thriftType, thriftFieldName, capitalizedThrift);
        Method thriftSetter = findSetter(thriftType, thriftFieldName, capitalizedThrift);
        Method pojoGetter = findGetter(pojoType, pojoField.getName(), capitalizedPojo);
        Method pojoSetter = findSetter(pojoType, pojoField.getName(), capitalizedPojo);

        if (thriftGetter == null || thriftSetter == null || pojoGetter == null || pojoSetter == null) {
            LoggerUtil.debug("字段 {} 的 getter/setter 不完整，跳过映射", thriftFieldName);
            return null;
        }

        return new FieldMapping(
                lookup.unreflect(thriftGetter),
                lookup.unreflect(thriftSetter),
                lookup.unreflect(pojoGetter),
                lookup.unreflect(pojoSetter),
                thriftGetter.getReturnType(),
                pojoField.getType()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<? extends TFieldIdEnum, FieldMetaData> getThriftMetaDataMap() {
        try {
            Field metaField = thriftType.getField("metaDataMap");
            return (Map<? extends TFieldIdEnum, FieldMetaData>) metaField.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static Method findGetter(Class<?> clazz, String fieldName, String capitalized) {
        // Thrift: getXxx() 或 isXxx()
        try {
            return clazz.getMethod("get" + capitalized);
        } catch (NoSuchMethodException e1) {
            try {
                return clazz.getMethod("is" + capitalized);
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
    }

    private static Method findSetter(Class<?> clazz, String fieldName, String capitalized) {
        String setterName = "set" + capitalized;
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    /**
     * 基本类型转换（int ↔ long、short ↔ int 等）
     */
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        Class<?> sourceType = value.getClass();
        if (targetType.isAssignableFrom(sourceType)) {
            return value;
        }

        if (value instanceof Number number) {
            if (targetType == int.class || targetType == Integer.class) {
                return number.intValue();
            } else if (targetType == long.class || targetType == Long.class) {
                return number.longValue();
            } else if (targetType == short.class || targetType == Short.class) {
                return number.shortValue();
            } else if (targetType == byte.class || targetType == Byte.class) {
                return number.byteValue();
            } else if (targetType == float.class || targetType == Float.class) {
                return number.floatValue();
            } else if (targetType == double.class || targetType == Double.class) {
                return number.doubleValue();
            }
        }

        return value;
    }

    /**
     * snake_case → camelCase
     */
    private static String toCamelCase(String snakeCase) {
        if (snakeCase == null || !snakeCase.contains("_")) {
            return snakeCase;
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 字段映射信息，包含双向的 MethodHandle 访问器
     */
    private record FieldMapping(
            MethodHandle thriftGetter,
            MethodHandle thriftSetter,
            MethodHandle pojoGetter,
            MethodHandle pojoSetter,
            Class<?> thriftFieldType,
            Class<?> pojoFieldType
    ) {
    }
}
