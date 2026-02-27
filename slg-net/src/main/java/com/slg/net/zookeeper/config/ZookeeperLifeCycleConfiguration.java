package com.slg.net.zookeeper.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.net.zookeeper.service.ZookeeperShareService;
import com.slg.net.zookeeper.util.ZookeeperConnectionValidator;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;

/**
 * Zookeeper 生命周期配置
 * 通过 @EnableZookeeper 注解自动引入
 *
 * <p>负责管理 CuratorFramework 的启动与关闭，
 * 启动时验证连接、加载 ZK 全量数据并启动监听
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class ZookeeperLifeCycleConfiguration {

    /**
     * 抑制 ZooKeeper 和 Curator 内部的 INFO 日志（如 Client environment 等），
     * 仅保留 WARN 及以上级别
     */
    private void suppressZookeeperLogs() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("org.apache.zookeeper").setLevel(Level.WARN);
        loggerContext.getLogger("org.apache.curator").setLevel(Level.WARN);
    }

    @Bean
    public SmartLifecycle zookeeperLifeCycle(CuratorFramework curatorFramework,
                                             ZookeeperConnectionValidator validator,
                                             ZookeeperShareService shareService) {
        return new SmartLifecycle() {

            private volatile boolean running = false;

            @Override
            public void start() {
                suppressZookeeperLogs();

                curatorFramework.start();

                if (!validator.validateConnection()) {
                    LoggerUtil.error("Zookeeper 连接失败，服务器启动终止！");
                    throw new RuntimeException("Zookeeper 连接失败");
                }

                shareService.loadAll();
                shareService.watchAll();

                running = true;
            }

            @Override
            public void stop() {
                shareService.unwatchAll();
                curatorFramework.close();
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }

            @Override
            public int getPhase() {
                return LifecyclePhase.ZOOKEEPER;
            }
        };
    }
}
