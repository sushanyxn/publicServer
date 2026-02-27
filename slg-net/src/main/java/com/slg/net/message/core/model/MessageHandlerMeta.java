package com.slg.net.message.core.model;

import lombok.Getter;
import lombok.Setter;

import java.lang.invoke.MethodHandle;

/**
 * 消息处理器元信息
 * 存储被 @MessageHandler 注解标注的方法的元信息
 * 
 * @author yangxunan
 * @date 2026/01/22
 */
@Getter
@Setter
public class MessageHandlerMeta {
    
    /**
     * 消息类型（协议类）
     */
    private final Class<?> messageType;
    
    /**
     * 处理器所在的 Bean 实例
     */
    private final Object handlerBean;
    
    /**
     * 处理器方法的 MethodHandle
     */
    private final MethodHandle methodHandle;
    
    /**
     * 方法名（用于日志和调试）
     */
    private final String methodName;
    
    /**
     * Bean 名称（用于日志和调试）
     */
    private final String beanName;

    /**
     * 是否需要主体 默认第三个字段是操作主体，各个进程可能不一样
     */
    private final boolean needOwner;
    
    public MessageHandlerMeta(Class<?> messageType, Object handlerBean, 
                             MethodHandle methodHandle, String methodName, String beanName, boolean needOwner) {
        this.messageType = messageType;
        this.handlerBean = handlerBean;
        this.methodHandle = methodHandle;
        this.methodName = methodName;
        this.beanName = beanName;
        this.needOwner = needOwner;
    }

    @Override
    public String toString() {
        return "MessageHandlerMeta{" +
                "messageType=" + messageType.getSimpleName() +
                ", beanName='" + beanName + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
