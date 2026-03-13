package com.slg.client;

import com.slg.client.core.account.AccountManager;
import com.slg.client.core.config.ClientConfig;
import com.slg.client.core.module.ClientModuleManager;
import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Spring 上下文工具类
 * 提供静态方式访问 Spring Bean
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class SpringContext implements ApplicationContextAware {

    @Getter
    private static SpringContext instance;

    @Getter
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        instance = this;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            this.applicationContext = applicationContext;
            for (Field field : SpringContext.class.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!java.lang.reflect.Modifier.isStatic(modifiers)) {
                    continue;
                }
                if (field.getName().equals("applicationContext")
                        || field.getName().equals("instance")) {
                    continue;
                }
                Class<?> type = field.getType();
                try {
                    Object bean = applicationContext.getBean(type);
                    field.setAccessible(true);
                    field.set(null, bean);
                } catch (Exception e) {
                    LoggerUtil.warn("SpringContext 注入字段失败，field={}，该类可能未配置", field.getName(), e);
                }
            }
            LoggerUtil.debug("SpringContext 初始化完成，已注入核心 Bean");
        } catch (Exception ex) {
            LoggerUtil.error("SpringContext 初始化失败", ex);
            throw ex;
        }
    }

    @Getter
    private static ClientConfig clientConfig;
    @Getter
    private static AccountManager accountManager;
    @Getter
    private static ClientModuleManager clientModuleManager;
    @Getter
    private static Executor executor;
}
