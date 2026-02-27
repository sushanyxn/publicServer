package com.slg.game.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 服务器基础信息配置类
 * 用于读取和管理服务器的基础配置信息
 *
 * @author yangxunan
 * @date 2026/01/26
 */
@Component
@ConfigurationProperties(prefix = "server.game")
@Getter
@Setter
public class GameServerConfiguration {

    /**
     * 服务器ID
     */
    private int serverId;

    /**
     * 绑定的服务器ID
     */
    private int bindSceneId;

    /**
     * 绑定的服务器地址
     */
    private String bindSceneUrl;

}

