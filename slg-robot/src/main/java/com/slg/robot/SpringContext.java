package com.slg.robot;

import com.slg.common.log.LoggerUtil;
import com.slg.robot.core.config.RobotConfig;
import com.slg.common.executor.Executor;
import com.slg.robot.core.manager.RobotManager;
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
 * @date 2026/01/22
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
                    // Bean 可能不存在或为可选，记录警告但继续
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
    private static RobotConfig robotConfig;
    @Getter
    private static RobotManager robotManager;
    @Getter
    private static Executor executor;
}

