package com.slg.common.event.model;

import com.slg.common.log.LoggerUtil;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件元数据
 * 存储某个事件类型的所有监听器信息
 * 
 * @author yangxunan
 * @date 2026/1/28
 */
public class EventMeta {

    /**
     * 事件监听器列表，使用线程安全的列表
     */
    private final List<EventListenerWrapper> listeners = new CopyOnWriteArrayList<>();

    /**
     * 注册事件监听器
     * 
     * @param wrapper 事件监听器包装器
     */
    public void addListener(EventListenerWrapper wrapper) {
        listeners.add(wrapper);
        // 按照 order 排序，order 越小优先级越高
        listeners.sort(Comparator.comparingInt(EventListenerWrapper::getOrder));
    }

    /**
     * 触发所有监听器
     * 
     * @param event 事件对象
     */
    public void fireEvent(IEvent event) {
        for (EventListenerWrapper wrapper : listeners) {
            try {
                wrapper.invoke(event);
            } catch (Throwable e) {
                LoggerUtil.error("执行事件监听器异常: event=" + event.getClass().getSimpleName() + ", listener=" + wrapper, e);
            }
        }
    }

    /**
     * 获取监听器数量
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
