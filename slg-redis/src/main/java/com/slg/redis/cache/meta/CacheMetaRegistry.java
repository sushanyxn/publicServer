package com.slg.redis.cache.meta;

import com.slg.common.log.LoggerUtil;
import com.slg.redis.cache.CacheModule;
import com.slg.redis.cache.anno.CacheEntity;
import com.slg.redis.cache.anno.CacheField;
import com.slg.redis.cache.codec.ICacheFieldCodec;
import com.slg.redis.cache.codec.JsonCacheFieldCodec;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 缓存元数据注册中心
 * 启动时扫描所有 @CacheEntity 类，构建元数据映射
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class CacheMetaRegistry {

    /** 扫描的基础包路径 */
    private static final String BASE_PACKAGE = "com.slg";

    /** entityClass -> CacheEntityMeta */
    private final Map<Class<?>, CacheEntityMeta> classMetas = new HashMap<>();

    /** CacheModule -> CacheEntityMeta */
    private final Map<CacheModule, CacheEntityMeta> moduleMetas = new HashMap<>();

    /** 自定义 Codec 实例缓存：codecClass -> instance */
    private final Map<Class<?>, ICacheFieldCodec<?>> codecInstances = new HashMap<>();

    /**
     * 执行扫描并构建元数据
     * 由 Spring 容器初始化时自动调用
     */
    @PostConstruct
    public void scan() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(CacheEntity.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(BASE_PACKAGE);
        for (BeanDefinition candidate : candidates) {
            try {
                Class<?> clazz = Class.forName(candidate.getBeanClassName());
                registerEntity(clazz);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("加载 @CacheEntity 类失败: " + candidate.getBeanClassName(), e);
            }
        }

        LoggerUtil.debug("缓存元数据扫描完成，共注册 {} 个缓存实体", classMetas.size());
    }

    /**
     * 注册单个缓存实体
     */
    private void registerEntity(Class<?> clazz) {
        CacheEntity annotation = clazz.getAnnotation(CacheEntity.class);
        if (annotation == null) {
            return;
        }

        CacheModule module = annotation.module();

        if (moduleMetas.containsKey(module)) {
            throw new IllegalStateException(String.format(
                    "CacheModule.%s 已被 %s 注册，不能再被 %s 使用。每个 CacheModule 只能对应一个 @CacheEntity",
                    module.name(), moduleMetas.get(module).getEntityClass().getName(), clazz.getName()));
        }

        verifyNoArgConstructor(clazz);

        Map<String, CacheFieldMeta> fieldMetas = scanFields(clazz);
        if (fieldMetas.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "@CacheEntity 类 %s 中没有找到任何 @CacheField 字段", clazz.getName()));
        }

        CacheEntityMeta meta = new CacheEntityMeta(module, clazz, fieldMetas);
        classMetas.put(clazz, meta);
        moduleMetas.put(module, meta);

        LoggerUtil.debug("注册缓存实体: {} -> CacheModule.{}, 字段数: {}",
                clazz.getSimpleName(), module.name(), fieldMetas.size());
    }

    /**
     * 扫描类中所有 @CacheField 字段
     */
    private Map<String, CacheFieldMeta> scanFields(Class<?> clazz) {
        Map<String, CacheFieldMeta> result = new LinkedHashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            CacheField cacheField = field.getAnnotation(CacheField.class);
            if (cacheField == null) {
                continue;
            }
            field.setAccessible(true);
            ICacheFieldCodec<?> codec = resolveCodec(cacheField.codec());
            CacheFieldMeta fieldMeta = new CacheFieldMeta(field.getName(), field.getType(), field, codec);
            result.put(field.getName(), fieldMeta);
        }
        return result;
    }

    /**
     * 获取或创建 Codec 实例
     */
    @SuppressWarnings("rawtypes")
    private ICacheFieldCodec<?> resolveCodec(Class<? extends ICacheFieldCodec> codecClass) {
        if (codecClass == ICacheFieldCodec.class) {
            return JsonCacheFieldCodec.INSTANCE;
        }
        ICacheFieldCodec<?> cached = codecInstances.get(codecClass);
        if (cached != null) {
            return cached;
        }
        try {
            ICacheFieldCodec<?> instance = codecClass.getDeclaredConstructor().newInstance();
            codecInstances.put(codecClass, instance);
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("创建自定义 Codec 失败: " + codecClass.getName(), e);
        }
    }

    /**
     * 校验类是否有无参构造器
     */
    private void verifyNoArgConstructor(Class<?> clazz) {
        try {
            clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format(
                    "@CacheEntity 类 %s 必须有无参构造器", clazz.getName()));
        }
    }

    /**
     * 根据实体类获取元数据
     *
     * @param entityClass 实体类
     * @return 元数据，不存在时返回 null
     */
    public CacheEntityMeta getMeta(Class<?> entityClass) {
        return classMetas.get(entityClass);
    }

    /**
     * 根据模块获取元数据
     *
     * @param module 缓存模块
     * @return 元数据，不存在时返回 null
     */
    public CacheEntityMeta getMeta(CacheModule module) {
        return moduleMetas.get(module);
    }

    /**
     * 获取所有已注册的元数据（不可变视图）
     *
     * @return entityClass -> CacheEntityMeta
     */
    public Map<Class<?>, CacheEntityMeta> getAllMetas() {
        return Collections.unmodifiableMap(classMetas);
    }
}
