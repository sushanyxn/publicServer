package com.slg.game;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.cache.manager.EntityCacheManager;
import com.slg.game.base.account.manager.AccountManager;
import com.slg.game.base.login.service.LoginService;
import com.slg.game.base.player.manager.PlayerManager;
import com.slg.game.base.player.service.PlayerService;
import com.slg.common.executor.Executor;
import com.slg.game.core.config.GameServerConfiguration;
import com.slg.game.develop.hero.service.HeroManager;
import com.slg.game.develop.hero.service.HeroService;
import com.slg.game.develop.task.manager.TaskManager;
import com.slg.game.net.manager.PlayerSessionManager;
import com.slg.net.socket.client.WebSocketClientManager;
import com.slg.table.manager.TableReloadManager;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @author yangxunan
 * @date 2025/12/23
 */
@Component("gameSpringContext")
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException{
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
                    // Bean 可能不存在或为可选（如 ConfigCenter），记录警告但继续
                    LoggerUtil.warn("SpringContext 注入字段失败，field={}，该类可能未配置", field.getName(), e);
                }
            }
            LoggerUtil.info("SpringContext 初始化完成，已注入核心 Bean");
        } catch (Exception ex) {
            LoggerUtil.error("SpringContext 初始化失败", ex);
            throw ex;
        }
    }

    @Getter
    private static EntityCacheManager entityCacheManager;
    @Getter
    private static AccountManager accountManager;
    @Getter
    private static PlayerManager playerManager;
    @Getter
    private static HeroManager heroManager;
    @Getter
    private static PlayerService playerService;
    @Getter
    private static HeroService heroService;
    @Getter
    private static TableReloadManager tableReloadManager;
    @Getter
    private static WebSocketClientManager webSocketClientManager;
    @Getter
    private static Executor executor;
    @Getter
    private static LoginService loginService;
    @Getter
    private static PlayerSessionManager playerSessionManager;
    @Getter
    private static TaskManager taskManager;
    @Getter
    private static GameServerConfiguration gameserverConfiguration;

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
