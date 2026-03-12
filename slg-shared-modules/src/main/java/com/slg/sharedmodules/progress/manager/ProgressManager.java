package com.slg.sharedmodules.progress.manager;

import com.slg.common.event.manager.EventBusManager;
import com.slg.common.event.model.EventListenerWrapper;
import com.slg.common.event.model.IEvent;
import com.slg.common.log.LoggerUtil;
import com.slg.sharedmodules.progress.bean.IProgressCondition;
import com.slg.sharedmodules.progress.model.IProgressEvent;
import com.slg.sharedmodules.progress.model.ProgressId;
import com.slg.sharedmodules.progress.model.ProgressMeta;
import com.slg.sharedmodules.progress.table.IProgressTable;
import com.slg.sharedmodules.progress.type.ProgressOwnerEnum;
import com.slg.sharedmodules.progress.type.ProgressTypeEnum;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进度管理器 - 基于事件驱动的进度管理系统
 * 通过监听进度事件来自动更新进度，支持进度更新和完成回调
 * 设计为纯缓存系统，不涉及持久化，业务初始化时需要重新注册
 *
 * 核心特性：
 * 1. 自动扫描并注册所有 IProgressEvent 子类的监听器
 * 2. 支持多种进度拥有者（玩家、公会等）
 * 3. 进度更新与完成回调互斥执行
 * 4. 线程安全的缓存设计
 *
 * @author yangxunan
 * @date 2026/1/28
 */
@Component
@Getter
public class ProgressManager {

    @Autowired
    private EventBusManager eventBusManager;

    @Autowired
    private IProgressTypeTransform[] progressTypeTransforms;

    /**
     * 进度条件类型到事件类型的映射
     * 键：IProgressCondition实现类，值：对应的事件类型
     */
    private final Map<Class<? extends IProgressCondition<?, ?>>, Class<? extends IProgressEvent<?>>> condition2EventMap = new ConcurrentHashMap<>();

    @Getter
    private static ProgressManager instance;

    @PostConstruct
    public void init() {
        scanProgressConditions();
        scanAndRegisterProgressEvents();
        instance = this;
        LoggerUtil.debug("进度管理器初始化完成");
    }

    @SuppressWarnings("unchecked")
    public <T> void registerProgress(
            ProgressOwnerEnum progressOwnerEnum,
            long ownerId,
            T owner,
            IProgressTable table,
            ProgressMeta progressMeta) {

        if (table == null) {
            LoggerUtil.warn("注册进度失败: 进度表配置为空");
            return;
        }

        IProgressCondition<T, ?> progressCondition = table.getProgressCondition();
        if (progressCondition == null) {
            LoggerUtil.warn("注册进度失败: 进度条件为空, progressId={}", table.getProgressId());
            return;
        }

        Class<? extends IProgressEvent<?>> eventType = condition2EventMap.get(progressCondition.getClass());
        if (eventType == null) {
            LoggerUtil.error("注册进度失败: 未找到进度条件对应的事件类型, conditionClass={}",
                    progressCondition.getClass().getSimpleName());
            return;
        }

        ProgressId progressId = new ProgressId(table.getProgressType(), table.getProgressId());

        try {
            if (progressMeta.getProgress() <= 0) {
                progressCondition.init(owner, progressMeta);
                if (progressMeta.getProgress() >= progressCondition.getFinishProgress()) {
                    progressMeta.triggerFinish();
                    return;
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("进度条件初始化失败: type = {}, owner={}, progressId={}", progressOwnerEnum.name(), ownerId, progressId, e);
        }

        Map<Class<? extends IEvent>, Map<Long, Map<ProgressId, ProgressMeta>>> event2Owner2ProgressMap = progressOwnerEnum.getEvent2Owner2ProgressMap();
        Map<ProgressId, ProgressMeta> progressMap = event2Owner2ProgressMap
                .computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(ownerId, k -> new ConcurrentHashMap<>());

        ProgressMeta existingMeta = progressMap.put(progressId, progressMeta);
        if (existingMeta != null) {
            LoggerUtil.warn("进度重复注册，已覆盖旧进度: type = {}, owner={}, progressId={}, 旧进度={}, 新进度={}",
                    progressOwnerEnum.name(), ownerId, progressId,
                    existingMeta.getProgress(), progressMeta.getProgress());
        }
    }

    @SuppressWarnings("unchecked")
    public void onTrigger(IEvent event) {
        if (!(event instanceof IProgressEvent progressEvent)) {
            return;
        }

        Map<Class<? extends IEvent>, Map<Long, Map<ProgressId, ProgressMeta>>> event2Owner2ProgressMap = progressEvent.getOwnerEnum().getEvent2Owner2ProgressMap();
        Map<Long, Map<ProgressId, ProgressMeta>> ownerMap = event2Owner2ProgressMap.get(event.getClass());
        if (CollectionUtils.isEmpty(ownerMap)) {
            return;
        }

        Map<ProgressId, ProgressMeta> progressMap = ownerMap.get(progressEvent.getOwnerId());
        if (CollectionUtils.isEmpty(progressMap)) {
            return;
        }

        for (Map.Entry<ProgressId, ProgressMeta> entry : progressMap.entrySet()) {
            ProgressId progressId = entry.getKey();
            ProgressMeta progressMeta = entry.getValue();

            IProgressCondition<?, ?> condition = progressMeta.getProgressCondition();
            if (condition == null) {
                LoggerUtil.warn("找不到进度条件: type = {}, owner={}, progressId={}", progressEvent.getOwnerEnum().name(), progressEvent.getOwnerId(), progressId);
                continue;
            }

            try {
                long oldProgress = progressMeta.getProgress();
                @SuppressWarnings("unchecked")
                IProgressCondition<Object, IProgressEvent<Object>> uncheckedCondition =
                        (IProgressCondition<Object, IProgressEvent<Object>>) condition;
                @SuppressWarnings("unchecked")
                IProgressEvent<Object> uncheckedEvent = (IProgressEvent<Object>) progressEvent;
                uncheckedCondition.onEvent(uncheckedEvent, progressMeta);

                if (oldProgress != progressMeta.getProgress()) {
                    boolean isFinished = progressMeta.getProgress() >= condition.getFinishProgress();
                    if (isFinished) {
                        progressMeta.triggerFinish();
                    } else {
                        progressMeta.triggerUpdate();
                    }
                }
            } catch (Exception e) {
                LoggerUtil.error("处理进度事件异常: type = {}, owner={}, progressId={}",
                        progressEvent.getOwnerEnum().name(), progressEvent.getOwnerId(), progressId, e);
            }
        }
    }

    public void unregisterProgress(ProgressOwnerEnum progressOwnerEnum, long ownerId, ProgressMeta progressMeta) {
        ProgressId progressId = new ProgressId(progressMeta.getType(), progressMeta.getId());
        Map<Class<? extends IEvent>, Map<Long, Map<ProgressId, ProgressMeta>>> event2Owner2ProgressMap = progressOwnerEnum.getEvent2Owner2ProgressMap();
        for (Map<Long, Map<ProgressId, ProgressMeta>> ownerMap : event2Owner2ProgressMap.values()) {
            Map<ProgressId, ProgressMeta> progressMap = ownerMap.get(ownerId);
            if (progressMap != null) {
                progressMap.remove(progressId);
                if (progressMap.isEmpty()) {
                    ownerMap.remove(ownerId);
                }
            }
        }
    }

    public void clearOwnerProgress(ProgressOwnerEnum progressOwnerEnum, long ownerId) {
        Map<Class<? extends IEvent>, Map<Long, Map<ProgressId, ProgressMeta>>> event2Owner2ProgressMap = progressOwnerEnum.getEvent2Owner2ProgressMap();
        for (Map<Long, Map<ProgressId, ProgressMeta>> ownerMap : event2Owner2ProgressMap.values()) {
            ownerMap.remove(ownerId);
        }
        LoggerUtil.debug("清除拥有者所有进度: type = {}, owner={}", progressOwnerEnum.name(), ownerId);
    }

    private void scanProgressConditions() {
        try {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(IProgressCondition.class));
            String[] basePackages = {"com.slg"};
            int mappedCount = 0;
            for (String basePackage : basePackages) {
                Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
                for (BeanDefinition beanDefinition : candidateComponents) {
                    String className = beanDefinition.getBeanClassName();
                    if (className != null) {
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                                continue;
                            }
                            if (IProgressCondition.class.isAssignableFrom(clazz)) {
                                @SuppressWarnings("unchecked")
                                Class<? extends IProgressCondition<?, ?>> conditionClass =
                                        (Class<? extends IProgressCondition<?, ?>>) clazz;
                                Class<? extends IProgressEvent<?>> eventClass =
                                        extractEventTypeFromCondition(conditionClass);
                                if (eventClass != null) {
                                    condition2EventMap.put(conditionClass, eventClass);
                                    mappedCount++;
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            LoggerUtil.error("无法加载类: {}", className, e);
                        }
                    }
                }
            }
            LoggerUtil.debug("进度条件映射完成，共映射 {} 个条件类型", mappedCount);
        } catch (Exception e) {
            LoggerUtil.error("扫描进度条件类时发生错误", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends IProgressEvent<?>> extractEventTypeFromCondition(
            Class<? extends IProgressCondition<?, ?>> conditionClass) {
        try {
            Type[] genericInterfaces = conditionClass.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                    Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                    if (IProgressCondition.class.isAssignableFrom(rawType)) {
                        Type[] typeArguments = parameterizedType.getActualTypeArguments();
                        for (Type typeArgument : typeArguments) {
                            if (typeArgument instanceof Class) {
                                Class<?> eventClass = (Class<?>) typeArgument;
                                if (IProgressEvent.class.isAssignableFrom(eventClass)) {
                                    return (Class<? extends IProgressEvent<?>>) eventClass;
                                }
                            }
                        }
                    }
                }
            }
            Class<?> superClass = conditionClass.getSuperclass();
            if (superClass != null && IProgressCondition.class.isAssignableFrom(superClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends IProgressCondition<?, ?>> superConditionClass =
                        (Class<? extends IProgressCondition<?, ?>>) superClass;
                return extractEventTypeFromCondition(superConditionClass);
            }
            LoggerUtil.warn("无法从进度条件中提取事件类型: {}", conditionClass.getSimpleName());
        } catch (Exception e) {
            LoggerUtil.error("提取事件类型时发生异常: {}", conditionClass.getSimpleName(), e);
        }
        return null;
    }

    private void scanAndRegisterProgressEvents() {
        try {
            ClassPathScanningCandidateComponentProvider scanner =
                    new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(IProgressEvent.class));
            String[] basePackages = {"com.slg"};
            int registeredCount = 0;
            for (String basePackage : basePackages) {
                Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
                for (BeanDefinition beanDefinition : candidateComponents) {
                    String className = beanDefinition.getBeanClassName();
                    if (className != null) {
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                                continue;
                            }
                            if (IProgressEvent.class.isAssignableFrom(clazz)) {
                                @SuppressWarnings("unchecked")
                                Class<? extends IProgressEvent<?>> eventClass = (Class<? extends IProgressEvent<?>>) clazz;
                                registerEventListener(eventClass);
                                registeredCount++;
                            }
                        } catch (ClassNotFoundException e) {
                            LoggerUtil.error("无法加载类: {}", className, e);
                        }
                    }
                }
            }
            LoggerUtil.debug("进度事件监听器注册完成，共注册 {} 个事件类型", registeredCount);
        } catch (Exception e) {
            LoggerUtil.error("扫描进度事件类时发生错误", e);
        }
    }

    private void registerEventListener(Class<? extends IProgressEvent<?>> eventClass) {
        try {
            Method method = ProgressManager.class.getDeclaredMethod("onTrigger", IEvent.class);
            EventListenerWrapper wrapper = new EventListenerWrapper(this, method, 100000);
            eventBusManager.registerListener(eventClass, wrapper);
        } catch (NoSuchMethodException e) {
            LoggerUtil.error("无法找到 onTrigger 方法", e);
        }
    }

    public ProgressTypeEnum getProgressType(int type) {
        for (IProgressTypeTransform progressTypeTransform : progressTypeTransforms) {
            ProgressTypeEnum progressTypeEnum = progressTypeTransform.getProgressType(type);
            if (progressTypeEnum != null) {
                return progressTypeEnum;
            }
        }
        return null;
    }
}
