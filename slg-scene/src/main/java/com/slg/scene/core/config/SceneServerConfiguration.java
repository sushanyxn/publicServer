package com.slg.scene.core.config;

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
@ConfigurationProperties(prefix = "server.scene")
@Getter
@Setter
public class SceneServerConfiguration {

    /**
     * 服务器ID
     */
    private Integer serverId;

}

