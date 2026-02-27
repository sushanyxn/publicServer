package com.slg.scene.core.config;

import com.slg.net.zookeeper.service.ZookeeperShareService;
import com.slg.net.zookeeper.writer.SceneServerZkWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Scene 模块 ZK Writer Bean 注册
 * 将 SceneServerZkWriter 注册为 Spring Bean，绑定本服 serverId
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Configuration
public class SceneZookeeperBeanConfiguration {

    @Bean
    public SceneServerZkWriter sceneServerZkWriter(ZookeeperShareService shareService,
                                                    SceneServerConfiguration config) {
        return new SceneServerZkWriter(config.getServerId(), shareService);
    }
}
