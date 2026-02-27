package com.slg.entity.mongo.config;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.entity.mongo.util.MongoConnectionValidator;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;

/**
 * MongoDB 生命周期配置
 * 通过 @EnableMongo 注解自动引入
 *
 * <p>负责在服务启动时验证 MongoDB 连接，确保数据库可用
 *
 * @author yangxunan
 * @date 2026/02/24
 */
public class MongoLifeCycleConfiguration {

    @Bean
    public SmartLifecycle mongoConnectionLifeCycle(MongoConnectionValidator validator) {
        return new SmartLifecycle() {

            private volatile boolean running = false;

            @Override
            public void start() {
                if (!validator.validateConnection()) {
                    LoggerUtil.error("MongoDB 连接失败，服务器启动终止！");
                    throw new RuntimeException("MongoDB 连接失败");
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
