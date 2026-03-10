package com.slg.scene;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.cache.manager.EntityCacheManager;
import com.slg.scene.base.manager.ScenePlayerManager;
import com.slg.scene.base.manager.SceneIdGeneratorManager;
import com.slg.scene.scene.base.manager.SceneManager;
import com.slg.scene.scene.base.service.SceneService;
import com.slg.net.socket.client.WebSocketClientManager;
import com.slg.common.executor.Executor;
import com.slg.table.manager.TableReloadManager;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Spring 上下文管理类
 * 负责管理和注入核心 Bean
 *
 * @author yangxunan
 * @date 2026/02/02
 */
@Component("sceneSpringContext")
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
            LoggerUtil.info("SpringContext 初始化完成，已注入核心 Bean");
        } catch (Exception ex) {
            LoggerUtil.error("SpringContext 初始化失败", ex);
            throw ex;
        }
    }

    // 核心 Bean 静态引用
    @Getter
    private static EntityCacheManager entityCacheManager;
    @Getter
    private static ScenePlayerManager scenePlayerManager;
    @Getter
    private static SceneService sceneService;
    @Getter
    private static TableReloadManager tableReloadManager;
    @Getter
    private static WebSocketClientManager webSocketClientManager;
    @Getter
    private static Executor executor;
    @Getter
    private static SceneManager sceneManager;
    @Getter
    private static SceneIdGeneratorManager sceneIdGeneratorManager;

    /**
     * 获取 Spring ApplicationContext（静态便捷方法）。
     *
     * @return 当前应用的 ApplicationContext，未初始化时返回 null
     */
    public static ApplicationContext getContext() {
        return instance != null ? instance.getApplicationContext() : null;
    }

    /**
     * 根据类型获取 Bean。
     *
     * @param requiredType Bean 类型
     * @param <T>           类型泛型
     * @return 对应类型的 Bean 实例
     * @throws org.springframework.beans.BeansException 若 Bean 不存在或上下文未就绪
     */
    public static <T> T getBean(Class<T> requiredType) {
        ApplicationContext ctx = getContext();
        if (ctx == null) {
            throw new IllegalStateException("SpringContext 尚未初始化，无法 getBean");
        }
        return ctx.getBean(requiredType);
    }
}
