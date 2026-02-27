package com.slg.robot.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 机器人测试配置
 * 用于配置机器人客户端的测试参数
 *
 * @author yangxunan
 * @date 2026/01/22
 */
@Data
@Component
@ConfigurationProperties(prefix = "robot")
public class RobotConfig {

    /**
     * 服务器地址
     */
    private String serverUrl = "ws://localhost:50001";

}

