package com.slg.entity.mysql.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.entity.mysql.util.MysqlConnectionValidator;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;

/**
 * MySQL 生命周期配置
 * 通过 {@code @EnableMysql} 注解自动引入
 *
 * <p>负责在服务启动时验证 MySQL 连接，确保数据库可用
 *
 * @author yangxunan
 * @date 2026/02/24
 */
public class MysqlLifeCycleConfiguration {

    @Bean
    public SmartLifecycle mysqlConnectionLifeCycle(MysqlConnectionValidator validator) {
        return new SmartLifecycle() {

            private volatile boolean running = false;

            @Override
            public void start() {
                if (!validator.validateConnection()) {
                    LoggerUtil.error("MySQL 连接失败，服务器启动终止！");
                    throw new RuntimeException("MySQL 连接失败");
                }
                running = true;
            }

            @Override
            public void stop() {
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }

            @Override
            public int getPhase() {
                return LifecyclePhase.DATABASE;
            }
        };
    }
}
