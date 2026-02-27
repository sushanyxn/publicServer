package com.slg.redis.cache.accessor;

import com.slg.redis.cache.anno.CacheAccessorInject;
import com.slg.redis.cache.meta.CacheEntityMeta;
import com.slg.redis.cache.meta.CacheMetaRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存访问器管理器
 * 负责创建和管理 {@link CacheAccessor} 实例，同时作为 {@link BeanPostProcessor}
 * 处理 {@link CacheAccessorInject} 注解的自动注入
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class CacheAccessorManager implements BeanPostProcessor {

    @Lazy
    @Autowired
    private CacheMetaRegistry metaRegistry;

    @Lazy
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Lazy
    @Autowired
    private RedisConnectionFactory connectionFactory;

    /** entityClass -> CacheAccessor 实例缓存 */
    private final Map<Class<?>, CacheAccessor<?>> accessorCache = new ConcurrentHashMap<>();

    /**
     * 获取指定实体类型的 CacheAccessor
     *
     * @param entityClass 缓存实体类
     * @param <T>         实体类型
     * @return CacheAccessor 实例
     */
    @SuppressWarnings("unchecked")
    public <T> CacheAccessor<T> getAccessor(Class<T> entityClass) {
        return (CacheAccessor<T>) accessorCache.computeIfAbsent(entityClass, clazz -> {
            CacheEntityMeta meta = metaRegistry.getMeta(clazz);
            if (meta == null) {
                throw new IllegalStateException(String.format(
                        "类 %s 未标注 @CacheEntity 或未被扫描到", clazz.getName()));
            }
            return new CacheAccessor<>(meta, stringRedisTemplate, connectionFactory);
        });
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            if (field.isAnnotationPresent(CacheAccessorInject.class)) {
                injectAccessor(bean, field);
            }
        });
        return bean;
    }

    /**
     * 注入 CacheAccessor 实例
     */
    private void injectAccessor(Object bean, Field field) {
        if (!CacheAccessor.class.equals(field.getType())) {
            throw new IllegalStateException(String.format(
                    "@CacheAccessorInject 只能用于 CacheAccessor 类型字段。发现字段: %s.%s，类型: %s",
                    bean.getClass().getName(), field.getName(), field.getType().getName()));
        }

        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException(String.format(
                    "CacheAccessor 字段必须指定泛型类型参数。发现字段: %s.%s",
                    bean.getClass().getName(), field.getName()));
        }

        Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
        if (actualTypeArgs.length != 1 || !(actualTypeArgs[0] instanceof Class<?>)) {
            throw new IllegalStateException(String.format(
                    "CacheAccessor 必须有且仅有一个具体类型参数。发现字段: %s.%s",
                    bean.getClass().getName(), field.getName()));
        }

        Class<?> entityClass = (Class<?>) actualTypeArgs[0];
        CacheAccessor<?> accessor = getAccessor(entityClass);

        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, bean, accessor);
    }
}
