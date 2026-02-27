package com.slg.net.zookeeper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zookeeper 连接配置属性
 * 对应 application.yml 中 {@code zookeeper} 前缀的配置
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@ConfigurationProperties(prefix = "zookeeper")
@Getter
@Setter
public class ZookeeperProperties {

    /** Zookeeper 连接字符串（格式：host1:port1,host2:port2） */
    private String connectString = "localhost:2181";

    /** 会话超时时间（毫秒） */
    private int sessionTimeout = 30000;

    /** 连接超时时间（毫秒） */
    private int connectionTimeout = 10000;

    /** 基础路径前缀，所有操作基于此路径 */
    private String basePath = "/slg";

    /** 重试配置 */
    private Retry retry = new Retry();

    /**
     * 重试策略配置
     */
    @Getter
    @Setter
    public static class Retry {

        /** 最大重试次数 */
        private int count = 3;

        /** 重试间隔基础时间（毫秒），实际使用 ExponentialBackoffRetry */
        private int interval = 1000;
    }
}
