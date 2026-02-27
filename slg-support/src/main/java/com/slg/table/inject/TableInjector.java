package com.slg.table.inject;

import com.slg.table.anno.Table;
import com.slg.table.manager.TableManager;
import com.slg.table.model.AbstractTable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 配置表自动注入处理器
 * 扫描所有 Spring Bean，为标记了 @Table 注解的字段自动注入对应的配置表实例
 *
 * @author yangxunan
 * @date 2025-12-29
 */
@Component
public class TableInjector implements BeanPostProcessor {

    @Lazy
    @Autowired
    private TableManager tableManager;

    /**
     * Bean 初始化之前处理
     * 扫描并注入 @Table 标记的字段
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        
        // 遍历所有字段
        ReflectionUtils.doWithFields(clazz, field -> {
            // 检查字段是否标记了 @Table 注解
            if (field.isAnnotationPresent(Table.class)) {
                injectTable(bean, field);
            }
        });
        
        return bean;
    }

    /**
     * 注入配置表实例
     *
     * @param bean 目标 Bean
     * @param field 目标字段
     */
    private void injectTable(Object bean, Field field) {
        // 检查字段类型是否为 AbstractTable 或其子类
        if (!AbstractTable.class.isAssignableFrom(field.getType())) {
            throw new IllegalStateException(
                    String.format("@Table 注解只能用于 AbstractTable 类型字段。" +
                            "发现字段: %s.%s，类型为: %s",
                            bean.getClass().getName(), field.getName(), field.getType().getName()));
        }
        
        // 获取泛型参数（配置类型）
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    String.format("配置表字段必须指定泛型类型参数。" +
                            "发现字段: %s.%s",
                            bean.getClass().getName(), field.getName()));
        }
        
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        
        if (actualTypeArguments.length != 1) {
            throw new IllegalStateException(
                    String.format("配置表必须有且仅有一个类型参数。" +
                            "发现字段: %s.%s",
                            bean.getClass().getName(), field.getName()));
        }
        
        // 获取配置类
        Class<?> configClass = (Class<?>) actualTypeArguments[0];
        
        // 验证配置类是否有 @Table 注解
        if (!configClass.isAnnotationPresent(Table.class)) {
            throw new IllegalStateException(
                    String.format("配置类必须标注 @Table 注解。" +
                            "发现配置类: %s，字段: %s.%s",
                            configClass.getName(), bean.getClass().getName(), field.getName()));
        }
        
        try {
            // 从 TableManager 注册并获取配置表实例
            @SuppressWarnings("unchecked")
            AbstractTable<?> table = tableManager.registerTable((Class) configClass);
            
            // 注入字段
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, table);

        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("配置表注入失败: %s.%s，配置类: %s",
                            bean.getClass().getName(), field.getName(), configClass.getName()), e);
        }
    }
}

