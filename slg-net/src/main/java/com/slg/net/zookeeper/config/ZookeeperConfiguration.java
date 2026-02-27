package com.slg.net.zookeeper.config;

import com.slg.common.log.LoggerUtil;
import com.slg.net.zookeeper.model.ZKConfig;
import com.slg.net.zookeeper.service.ZookeeperConfigService;
import com.slg.net.zookeeper.service.ZookeeperShareService;
import com.slg.net.zookeeper.util.ZookeeperConnectionValidator;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Zookeeper 配置类
 * 通过 @EnableZookeeper 注解自动引入
 *
 * <p>配置内容：
 * <ul>
 *   <li>CuratorFramework 客户端（指数退避重试策略）</li>
 *   <li>配置读取服务、信息共享服务和中心数据持有者</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@EnableConfigurationProperties(ZookeeperProperties.class)
@Import({
        ZookeeperConnectionValidator.class,
        ZKConfig.class,
        ZookeeperConfigService.class,
        ZookeeperShareService.class
})
public class ZookeeperConfiguration {

    /**
     * 创建 CuratorFramework 客户端
     * 使用 ExponentialBackoffRetry 指数退避重试策略
     *
     * <p>注意：此处仅创建客户端实例，不调用 start()，
     * 启动由 ZookeeperLifeCycleConfiguration 管理
     *
     * @param properties Zookeeper 配置属性
     * @return CuratorFramework 客户端实例
     */
    @Bean
    public CuratorFramework curatorFramework(ZookeeperProperties properties) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(
                properties.getRetry().getInterval(),
                properties.getRetry().getCount()
        );

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(properties.getConnectString())
                .sessionTimeoutMs(properties.getSessionTimeout())
                .connectionTimeoutMs(properties.getConnectionTimeout())
                .namespace(trimLeadingSlash(properties.getBasePath()))
                .retryPolicy(retryPolicy)
                .build();

        LoggerUtil.debug("CuratorFramework 客户端创建完成: {}, namespace={}",
                properties.getConnectString(), properties.getBasePath());
        return client;
    }

    /**
     * 去除路径前导斜杠（Curator namespace 不需要前导斜杠）
     */
    private String trimLeadingSlash(String path) {
        if (path != null && path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
}
