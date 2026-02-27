package com.slg.net.rpc.inject;

import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.anno.RpcRef;
import com.slg.net.rpc.manager.RpcProxyManager;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * RPC 代理注入器
 * 扫描并注入带 @RpcRef 注解的字段
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class RpcRefInjector implements BeanPostProcessor {

    private final RpcProxyManager rpcProxyManager;

    public RpcRefInjector(@Lazy RpcProxyManager rpcProxyManager) {
        this.rpcProxyManager = rpcProxyManager;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();

        // 遍历所有字段
        for (Field field : clazz.getDeclaredFields()) {
            // 查找 @RpcRef 注解
            if (!field.isAnnotationPresent(RpcRef.class)) {
                continue;
            }

            Class<?> fieldType = field.getType();

            // 检查是否是接口
            if (!fieldType.isInterface()) {
                LoggerUtil.error("[RPC] @RpcRef 只能用于接口类型: {}.{}", 
                        clazz.getSimpleName(), field.getName());
                throw new IllegalStateException("@RpcRef 只能用于接口类型: " + fieldType.getName());
            }

            try {
                // 获取或创建代理对象
                Object proxy = rpcProxyManager.getOrCreateProxy(fieldType);

                // 注入代理对象
                field.setAccessible(true);
                field.set(bean, proxy);

            } catch (IllegalAccessException e) {
                LoggerUtil.error("[RPC] 注入代理对象失败: {}.{}", 
                        clazz.getSimpleName(), field.getName(), e);
                throw new RuntimeException("注入 RPC 代理失败", e);
            }
        }

        return bean;
    }

}

