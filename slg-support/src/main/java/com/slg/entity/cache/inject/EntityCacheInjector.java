package com.slg.entity.cache.inject;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.manager.EntityCacheManager;
import com.slg.entity.cache.model.EntityCache;
import com.slg.entity.db.entity.BaseEntity;
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
 * EntityCache 自动注入处理器
 * 扫描所有 Spring Bean，为标记了 @InjectEntityCache 注解的字段自动注入对应的 EntityCache 实例
 *
 * @author yangxunan
 * @date 2025-12-18
 */
@Component
public class EntityCacheInjector implements BeanPostProcessor {

    @Lazy
    @Autowired
    private EntityCacheManager cacheManager;

    /**
     * Bean 初始化之前处理
     * 扫描并注入 @InjectEntityCache 标记的字段
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        
        // 遍历所有字段
        ReflectionUtils.doWithFields(clazz, field -> {
            // 检查字段是否标记了 @InjectEntityCache 注解
            if (field.isAnnotationPresent(EntityCacheInject.class)) {
                injectEntityCache(bean, field);
            }
        });
        
        return bean;
    }

    /**
     * 注入 EntityCache 实例
     *
     * @param bean 目标 Bean
     * @param field 目标字段
     */
    private void injectEntityCache(Object bean, Field field) {
        // 检查字段类型是否为 EntityCache
        if (!EntityCache.class.equals(field.getType())) {
            throw new IllegalStateException(
                    String.format("@EntityCacheInject 注解只能用于 EntityCache 类型字段。" +
                            "发现字段: %s.%s，类型为: %s",
                            bean.getClass().getName(), field.getName(), field.getType().getName()));
        }
        
        // 获取泛型参数（实体类型）
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    String.format("EntityCache 字段必须指定泛型类型参数。" +
                            "发现字段: %s.%s",
                            bean.getClass().getName(), field.getName()));
        }
        
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        
        if (actualTypeArguments.length != 1) {
            throw new IllegalStateException(
                    String.format("EntityCache 必须有且仅有一个类型参数。" +
                            "发现字段: %s.%s",
                            bean.getClass().getName(), field.getName()));
        }
        
        // 获取实体类
        Class<?> entityClass = (Class<?>) actualTypeArguments[0];
        
        // 验证实体类是否继承自 BaseEntity
        if (!BaseEntity.class.isAssignableFrom(entityClass)) {
            throw new IllegalStateException(
                    String.format("EntityCache 类型参数必须继承自 BaseEntity。" +
                            "发现实体类: %s，字段: %s.%s",
                            entityClass.getName(), bean.getClass().getName(), field.getName()));
        }
        
        // 从 EntityCacheManager 获取对应的缓存实例
        @SuppressWarnings("unchecked")
        EntityCache<?> cache = cacheManager.getCache((Class<? extends BaseEntity<?>>) entityClass);
        
        // 注入字段
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, bean, cache);
    }
}

