package com.slg.net.message.core.inject;

import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.message.core.manager.MessageHandlerManager;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * 消息处理器自动注入处理器
 * 扫描所有 Spring Bean，为标记了 @MessageHandler 注解的方法自动注册到 MessageHandlerManager
 * 
 * @author yangxunan
 * @date 2026/01/22
 */
@Component
public class MessageHandlerInjector implements BeanPostProcessor {
    
    @Lazy
    @Autowired
    private MessageHandlerManager messageHandlerManager;
    /**
     * Bean 初始化之前处理
     * 扫描并注册 @MessageHandler 标记的方法
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        
        // 遍历所有方法
        ReflectionUtils.doWithMethods(clazz, method -> {
            // 检查方法是否标记了 @MessageHandler 注解
            if (method.isAnnotationPresent(MessageHandler.class)) {
                registerMessageHandler(bean, method, beanName);
            }
        });
        
        return bean;
    }
    
    /**
     * Bean 初始化之后处理（不需要特殊处理）
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    
    /**
     * 注册消息处理器
     * 
     * @param bean 目标 Bean
     * @param method 目标方法
     * @param beanName Bean 名称
     */
    private void registerMessageHandler(Object bean, Method method, String beanName) {
        // 验证方法参数
        Class<?>[] paramTypes = method.getParameterTypes();
        
        if (paramTypes.length < 2) {
            throw new IllegalStateException(
                String.format("消息处理器方法参数数量错误: %s.%s，期望 >=2 个参数，实际 %d 个",
                    beanName, method.getName(), paramTypes.length));
        }
        
        // 验证第一个参数是 NetSession
        if (!NetSession.class.isAssignableFrom(paramTypes[0])) {
            throw new IllegalStateException(
                String.format("消息处理器方法第一个参数必须是 NetSession: %s.%s，实际类型: %s",
                    beanName, method.getName(), paramTypes[0].getName()));
        }

        boolean needOwner = paramTypes.length >= 3;
        
        // 第二个参数是消息类型
        Class<?> messageType = paramTypes[1];
        
        try {
            // 注册到 MessageHandlerManager
            messageHandlerManager.register(bean, method, messageType, beanName, needOwner);

        } catch (Exception e) {
            throw new IllegalStateException(
                String.format("注册消息处理器失败: %s.%s",
                    beanName, method.getName()), e);
        }
    }
}

