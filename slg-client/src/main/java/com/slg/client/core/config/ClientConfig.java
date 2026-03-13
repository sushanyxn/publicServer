package com.slg.client.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 客户端模拟器配置
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Data
@Component
@ConfigurationProperties(prefix = "client")
public class ClientConfig {

    /**
     * 游戏服务器 WebSocket 地址
     */
    private String serverUrl = "ws://localhost:50001/ws";
}
