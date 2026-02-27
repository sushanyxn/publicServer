package com.slg.common.event.inject;

import com.slg.common.event.anno.EventListener;
import com.slg.common.event.manager.EventBusManager;
import com.slg.common.event.model.EventListenerWrapper;
import com.slg.common.event.model.IEvent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 事件监听器 Bean 后处理器
 * 扫描所有 Spring Bean，查找标注了 @EventListener 注解的方法
 * 并将其注册到事件总线中
 * 
 * @author yangxunan
 * @date 2026/1/28
 */
@Component
public class EventListenerInjector implements BeanPostProcessor {

    @Lazy
    @Autowired
    private EventBusManager eventBusManager;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        // 扫描类中的所有方法
        Method[] methods = beanClass.getDeclaredMethods();
        for (Method method : methods) {
            // 查找 @EventListener 注解
            EventListener annotation = AnnotationUtils.findAnnotation(method, EventListener.class);
            if (annotation == null) {
                continue;
            }

            // 验证方法签名：必须有且仅有一个参数，且参数类型必须是 IEvent 的子类
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException(
                        String.format("事件监听器方法参数数量错误，应该有且仅有一个参数: bean=%s, method=%s",
                                beanClass.getSimpleName(), method.getName()));
            }

            Class<?> eventType = parameterTypes[0];
            if (!IEvent.class.isAssignableFrom(eventType)) {
                throw new IllegalArgumentException(
                        String.format("事件监听器方法参数类型错误，必须是 IEvent 的子类: bean=%s, method=%s, paramType=%s",
                                beanClass.getSimpleName(), method.getName(), eventType.getSimpleName()));
            }

            // 创建监听器包装器并注册到事件总线
            EventListenerWrapper wrapper = new EventListenerWrapper(
                    bean, 
                    method, 
                    annotation.order()
            );

            // 注册到事件总线
            @SuppressWarnings("unchecked")
            Class<? extends IEvent> eventClass = (Class<? extends IEvent>) eventType;
            eventBusManager.registerListener(eventClass, wrapper);
        }

        return bean;
    }
}
