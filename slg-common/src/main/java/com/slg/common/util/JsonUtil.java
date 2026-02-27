package com.slg.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.slg.common.log.LoggerUtil;

/**
 * JSON 工具类
 * 提供 JSON 序列化和反序列化的常用方法
 * 基于 Jackson 实现
 * 
 * @author yangxunan
 * @date 2025-12-23
 */
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 注册 Java 8 时间模块
        MAPPER.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许空对象
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param obj 要转换的对象
     * @return JSON 字符串，转换失败返回 null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            LoggerUtil.error("对象转JSON失败: {}", obj.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 将对象转换为格式化的 JSON 字符串（带缩进）
     *
     * @param obj 要转换的对象
     * @return 格式化的 JSON 字符串，转换失败返回 null
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            LoggerUtil.error("对象转格式化JSON失败: {}", obj.getClass().getName(), e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串转换为对象
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的对象，转换失败返回 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            LoggerUtil.error("JSON转对象失败: {}", clazz.getName(), e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串转换为对象（支持泛型）
     * 适用于 List、Map 等泛型集合
     *
     * @param json JSON 字符串
     * @param typeReference 类型引用
     * @param <T> 泛型类型
     * @return 转换后的对象，转换失败返回 null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            LoggerUtil.error("JSON转泛型对象失败", e);
            return null;
        }
    }

    /**
     * 对象深拷贝（通过 JSON 序列化/反序列化实现）
     *
     * @param obj 原对象
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 拷贝后的对象，拷贝失败返回 null
     */
    public static <T> T clone(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        try {
            String json = MAPPER.writeValueAsString(obj);
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            LoggerUtil.error("对象深拷贝失败: {}", clazz.getName(), e);
            return null;
        }
    }

    /**
     * 获取 ObjectMapper 实例
     * 用于自定义复杂操作
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}



