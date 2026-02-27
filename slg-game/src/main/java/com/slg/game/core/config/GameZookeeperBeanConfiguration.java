package com.slg.game.core.config;

import com.slg.net.zookeeper.service.ZookeeperShareService;
import com.slg.net.zookeeper.writer.GameServerZkWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Game 模块 ZK Writer Bean 注册
 * 将 GameServerZkWriter 注册为 Spring Bean，绑定本服 serverId
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Configuration
public class GameZookeeperBeanConfiguration {

    @Bean
    public GameServerZkWriter gameServerZkWriter(ZookeeperShareService shareService,
                                                  GameServerConfiguration config) {
        return new GameServerZkWriter(config.getServerId(), shareService);
    }
}
