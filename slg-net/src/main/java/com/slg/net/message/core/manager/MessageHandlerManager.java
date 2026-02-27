package com.slg.net.message.core.manager;

import com.slg.net.message.core.model.MessageHandlerMeta;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息处理器管理器
 * 管理所有已注册的消息处理器
 *
 * @author yangxunan
 * @date 2026/01/22
 */
@Component
public class MessageHandlerManager {

    /**
     * 消息类型 -> 处理器元信息
     */
    private final Map<Class<?>, MessageHandlerMeta> handlers = new ConcurrentHashMap<>();

    @Getter
    private static MessageHandlerManager instance;

    @PostConstruct
    public void init(){
        instance = this;
    }

    /**
     * 注册消息处理器
     *
     * @param bean        目标 Bean
     * @param method      目标方法
     * @param messageType 消息类型
     * @param beanName    Bean 名称
     */
    public void register(Object bean, Method method, Class<?> messageType, String beanName, boolean needOwner){
        // 验证消息类型是否已在 MessageRegistry 中注册
        MessageRegistry registry = MessageRegistry.getInstance();
        Integer protocolId = registry.getProtocolId(messageType);
        if (protocolId == null) {
            throw new IllegalStateException(
                    String.format("消息类型未注册: %s.%s 的参数类型 %s",
                            beanName, method.getName(), messageType.getName()));
        }

        // 检查是否已有处理器注册了该消息类型
        if (handlers.containsKey(messageType)) {
            MessageHandlerMeta existing = handlers.get(messageType);
            throw new IllegalStateException(
                    String.format("消息类型 %s 已被处理器 %s.%s 注册，不能重复注册到 %s.%s",
                            messageType.getSimpleName(),
                            existing.getBeanName(), existing.getMethodName(),
                            beanName, method.getName()));
        }

        try {
            // 创建 MethodHandle
            ReflectionUtils.makeAccessible(method);
            MethodHandle methodHandle = MethodHandles.lookup().unreflect(method);

            // 创建元信息
            MessageHandlerMeta meta = new MessageHandlerMeta(
                    messageType,
                    bean,
                    methodHandle,
                    method.getName(),
                    beanName,
                    needOwner
            );

            // 注册
            handlers.put(messageType, meta);

        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("创建消息处理器失败: %s.%s",
                            beanName, method.getName()), e);
        }
    }

    /**
     * 获取消息类型对应的处理器
     */
    public MessageHandlerMeta getHandler(Class<?> messageType){
        return handlers.get(messageType);
    }

    /**
     * 检查消息类型是否有对应的处理器
     */
    public boolean hasHandler(Class<?> messageType){
        return handlers.containsKey(messageType);
    }

    /**
     * 获取所有已注册的消息类型
     */
    public Map<Class<?>, MessageHandlerMeta> getAllHandlers(){
        return new ConcurrentHashMap<>(handlers);
    }
}
