package com.slg.common.event.model;

import com.slg.common.log.LoggerUtil;
import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * 事件监听器包装器
 * 封装监听器方法的调用逻辑，同步执行
 * 
 * @author yangxunan
 * @date 2026/1/28
 */
@Getter
public class EventListenerWrapper {

    /**
     * 监听器所属的 Bean 对象
     */
    private final Object bean;

    /**
     * 监听器方法
     */
    private final Method method;

    /**
     * 方法句柄，用于高效调用
     */
    private final MethodHandle methodHandle;

    /**
     * 执行顺序
     */
    private final int order;

    public EventListenerWrapper(Object bean, Method method, int order) {
        this.bean = bean;
        this.method = method;
        this.order = order;

        // 确保方法可访问
        method.setAccessible(true);

        // 创建方法句柄以提高调用效率
        try {
            this.methodHandle = MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    String.format("无法创建方法句柄: bean=%s, method=%s", 
                            bean.getClass().getSimpleName(), method.getName()), e);
        }
    }

    /**
     * 调用监听器方法
     * 
     * @param event 事件对象
     */
    public void invoke(IEvent event) {
        try {
            methodHandle.invoke(bean, event);
        } catch (Throwable e) {
            LoggerUtil.error("事件监听器执行异常: bean=" + bean.getClass().getSimpleName() + 
                    ", method=" + method.getName() + ", event=" + event.getClass().getSimpleName(), e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s.%s(order=%d)", 
                bean.getClass().getSimpleName(), method.getName(), order);
    }
}
