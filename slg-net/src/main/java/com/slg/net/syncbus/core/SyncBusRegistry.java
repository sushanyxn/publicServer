package com.slg.net.syncbus.core;

import com.slg.common.log.LoggerUtil;
import com.slg.net.syncbus.ISyncCache;
import com.slg.net.syncbus.ISyncCacheResolver;
import com.slg.net.syncbus.ISyncHolder;
import com.slg.net.syncbus.SyncModule;
import com.slg.net.syncbus.anno.SyncEntity;
import com.slg.net.syncbus.anno.SyncField;
import com.slg.net.syncbus.codec.ISyncFieldDecoder;
import com.slg.net.syncbus.codec.ISyncFieldEncoder;
import com.slg.net.syncbus.model.SyncCacheFieldMeta;
import com.slg.net.syncbus.model.SyncCacheMeta;
import com.slg.net.syncbus.model.SyncHolderFieldMeta;
import com.slg.net.syncbus.model.SyncHolderMeta;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 同步总线扫描注册中心
 * 启动时通过 ClassGraph 扫描 @SyncEntity 注解，构建 Holder/Cache 端的元数据
 * <p>
 * 同一进程可能只有 Holder 或只有 Cache（正常情况），不做跨角色配对校验
 *
 * @author yangxunan
 * @date 2026/02/12
 */
@Component
public class SyncBusRegistry implements InitializingBean {

    /** 扫描包路径 */
    private static final String SCAN_PACKAGE = "com.slg";

    /** Holder 端元数据：SyncModule -> SyncHolderMeta */
    private final Map<SyncModule, SyncHolderMeta> holderMetaMap = new EnumMap<>(SyncModule.class);

    /** Cache 端元数据：SyncModule -> SyncCacheMeta */
    private final Map<SyncModule, SyncCacheMeta> cacheMetaMap = new EnumMap<>(SyncModule.class);

    /** MethodHandles.Lookup 用于创建 MethodHandle */
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() {
        scanSyncEntities();
        collectResolvers();

        LoggerUtil.debug("[SyncBus] 扫描完成，Holder: {}，Cache: {}",
                holderMetaMap.size(), cacheMetaMap.size());
    }

    /**
     * 扫描所有标注了 @SyncEntity 的类，构建元数据
     */
    private void scanSyncEntities() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages(SCAN_PACKAGE)
                .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(SyncEntity.class)) {
                Class<?> clazz = classInfo.loadClass();
                SyncEntity syncEntity = clazz.getAnnotation(SyncEntity.class);
                if (syncEntity == null) {
                    continue;
                }

                SyncModule module = syncEntity.value();

                // 根据实现的接口区分角色
                if (ISyncHolder.class.isAssignableFrom(clazz)) {
                    buildHolderMeta(module, clazz);
                }
                if (ISyncCache.class.isAssignableFrom(clazz)) {
                    buildCacheMeta(module, clazz);
                }
            }

        } catch (Exception e) {
            LoggerUtil.error("[SyncBus] @SyncEntity 扫描异常", e);
            throw new RuntimeException("SyncBus 扫描失败", e);
        }
    }

    /**
     * 构建 Holder 端元数据
     *
     * @param module 同步模块
     * @param clazz  Holder 类
     */
    private void buildHolderMeta(SyncModule module, Class<?> clazz) {
        if (holderMetaMap.containsKey(module)) {
            throw new IllegalStateException(
                    String.format("[SyncBus] SyncModule %s 存在多个 Holder 类：%s 和 %s",
                            module, holderMetaMap.get(module).getHolderClass().getName(), clazz.getName()));
        }

        Map<String, SyncHolderFieldMeta> fieldMetaMap = new HashMap<>();

        // 遍历类及其父类的所有字段
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                SyncField syncField = field.getAnnotation(SyncField.class);
                if (syncField == null) {
                    continue;
                }

                String fieldName = field.getName();
                if (fieldMetaMap.containsKey(fieldName)) {
                    // 子类字段优先，跳过父类同名字段
                    continue;
                }

                try {
                    // 预编译 getter MethodHandle
                    field.setAccessible(true);
                    MethodHandle getter = LOOKUP.unreflectGetter(field);

                    // 实例化编码器（如果指定了自定义编码器）
                    ISyncFieldEncoder<?> encoder = null;
                    if (syncField.encoder() != ISyncFieldEncoder.class) {
                        encoder = syncField.encoder().getDeclaredConstructor().newInstance();
                    }

                    // 计算限流间隔（秒 -> 毫秒）
                    long syncIntervalMs = syncField.syncInterval() * 1000L;

                    SyncHolderFieldMeta fieldMeta = new SyncHolderFieldMeta(
                            fieldName, field.getType(), getter, encoder, syncIntervalMs);
                    fieldMetaMap.put(fieldName, fieldMeta);

                } catch (Exception e) {
                    LoggerUtil.error("[SyncBus] 构建 Holder 字段元数据失败: {}.{}",
                            clazz.getName(), fieldName, e);
                    throw new RuntimeException("构建 Holder 字段元数据失败: " + clazz.getName() + "." + fieldName, e);
                }
            }
        }

        SyncHolderMeta holderMeta = new SyncHolderMeta(module, clazz, fieldMetaMap);
        holderMetaMap.put(module, holderMeta);
    }

    /**
     * 构建 Cache 端元数据
     *
     * @param module 同步模块
     * @param clazz  Cache 类
     */
    private void buildCacheMeta(SyncModule module, Class<?> clazz) {
        if (cacheMetaMap.containsKey(module)) {
            throw new IllegalStateException(
                    String.format("[SyncBus] SyncModule %s 存在多个 Cache 类：%s 和 %s",
                            module, cacheMetaMap.get(module).getCacheClass().getName(), clazz.getName()));
        }

        Map<String, SyncCacheFieldMeta> fieldMetaMap = new HashMap<>();

        // 遍历类及其父类的所有字段
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                SyncField syncField = field.getAnnotation(SyncField.class);
                if (syncField == null) {
                    continue;
                }

                String fieldName = field.getName();
                if (fieldMetaMap.containsKey(fieldName)) {
                    // 子类字段优先，跳过父类同名字段
                    continue;
                }

                try {
                    // 预编译 setter MethodHandle
                    field.setAccessible(true);
                    MethodHandle setter = LOOKUP.unreflectSetter(field);

                    // 实例化解码器（如果指定了自定义解码器）
                    ISyncFieldDecoder<?> decoder = null;
                    if (syncField.decoder() != ISyncFieldDecoder.class) {
                        decoder = syncField.decoder().getDeclaredConstructor().newInstance();
                    }

                    SyncCacheFieldMeta fieldMeta = new SyncCacheFieldMeta(
                            fieldName, field.getType(), setter, decoder);
                    fieldMetaMap.put(fieldName, fieldMeta);

                } catch (Exception e) {
                    LoggerUtil.error("[SyncBus] 构建 Cache 字段元数据失败: {}.{}",
                            clazz.getName(), fieldName, e);
                    throw new RuntimeException("构建 Cache 字段元数据失败: " + clazz.getName() + "." + fieldName, e);
                }
            }
        }

        SyncCacheMeta cacheMeta = new SyncCacheMeta(module, clazz, fieldMetaMap);
        cacheMetaMap.put(module, cacheMeta);
    }

    /**
     * 从 Spring 容器收集所有 ISyncCacheResolver Bean，建立映射
     */
    @SuppressWarnings("rawtypes")
    private void collectResolvers() {
        Map<String, ISyncCacheResolver> resolverBeans = applicationContext.getBeansOfType(ISyncCacheResolver.class);
        for (ISyncCacheResolver resolver : resolverBeans.values()) {
            SyncModule module = resolver.getSyncModule();
            SyncCacheMeta cacheMeta = cacheMetaMap.get(module);
            if (cacheMeta != null) {
                cacheMeta.setResolver(resolver);
            }
        }

        // 检查有 Cache 类但无 Resolver 的情况
        for (Map.Entry<SyncModule, SyncCacheMeta> entry : cacheMetaMap.entrySet()) {
            if (entry.getValue().getResolver() == null) {
                LoggerUtil.error("[SyncBus] Cache 类 {} 对应的 SyncModule {} 没有找到 ISyncCacheResolver 实现",
                        entry.getValue().getCacheClass().getSimpleName(), entry.getKey());
            }
        }
    }

    /**
     * 获取 Holder 端元数据
     *
     * @param module 同步模块
     * @return Holder 元数据，不存在时返回 null
     */
    public SyncHolderMeta getHolderMeta(SyncModule module) {
        return holderMetaMap.get(module);
    }

    /**
     * 获取 Cache 端元数据
     *
     * @param module 同步模块
     * @return Cache 元数据，不存在时返回 null
     */
    public SyncCacheMeta getCacheMeta(SyncModule module) {
        return cacheMetaMap.get(module);
    }
}
