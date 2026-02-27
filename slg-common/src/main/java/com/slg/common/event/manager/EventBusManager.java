package com.slg.common.event.manager;

import com.slg.common.event.model.EventListenerWrapper;
import com.slg.common.event.model.EventMeta;
import com.slg.common.event.model.IEvent;
import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件总线管理器
 * 负责事件的注册、发布和分发
 * 使用单例模式，全局唯一
 *
 * @author yangxunan
 * @date 2026/1/28
 */
@Component
public class EventBusManager {

    /**
     * 事件类型 -> 事件元数据映射表
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<Class<? extends IEvent>, EventMeta> eventMetas = new ConcurrentHashMap<>();

    /**
     * 单例实例
     */
    @Getter
    private static EventBusManager instance;

    @PostConstruct
    public void init(){
        instance = this;
    }

    /**
     * 发布事件
     * 同步调用所有注册的监听器
     *
     * @param event 事件对象
     */
    public void publishEvent(IEvent event){
        if (event == null) {
            LoggerUtil.warn("发布事件失败: 事件对象为null");
            return;
        }

        Class<? extends IEvent> eventType = event.getClass();
        EventMeta meta = instance.eventMetas.get(eventType);

        if (meta == null) {
            LoggerUtil.debug("发布事件: {} (无监听器)", eventType.getSimpleName());
            return;
        }

        meta.fireEvent(event);
    }

    /**
     * 注册事件监听器
     *
     * @param eventType 事件类型
     * @param wrapper   事件监听器包装器
     */
    public void registerListener(Class<? extends IEvent> eventType, EventListenerWrapper wrapper){
        if (eventType == null || wrapper == null) {
            LoggerUtil.warn("注册事件监听器失败: eventType={}, wrapper={}", eventType, wrapper);
            return;
        }

        EventMeta meta = instance.eventMetas.computeIfAbsent(eventType, k -> new EventMeta());
        meta.addListener(wrapper);
    }

    /**
     * 获取事件元数据映射表（只读）
     */
    public Map<Class<? extends IEvent>, EventMeta> getEventMetas(){
        return eventMetas;
    }
}
