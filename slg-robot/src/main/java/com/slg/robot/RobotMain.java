package com.slg.robot;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.annotation.EnableWebSocketClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * 机器人测试客户端启动类
 * 用于模拟客户端对服务器进行压力测试和功能测试
 *
 * @author yangxunan
 * @date 2026/01/22
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.slg.common",       // 通用工具
    "com.slg.robot",        // 机器人客户端
    "com.slg.net.message",  // 协议解析
})
@EnableWebSocketClient
public class RobotMain {

    /**
     * 应用程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext context = null;

        try {
            LoggerUtil.debug("机器人测试客户端启动中...");

            // 启动 Spring Boot 应用
            context = SpringApplication.run(RobotMain.class, args);

            // 注册关闭钩子
            shutdownHook(context);

            LoggerUtil.debug("机器人测试客户端启动成功！");

        } catch (Exception e) {
            LoggerUtil.error("机器人测试客户端启动失败！", e);
            // 启动失败时，尝试关闭已创建的容器
            if (context != null) {
                try {
                    context.close();
                } catch (Exception ex) {
                    LoggerUtil.error("关闭容器时发生异常", ex);
                }
            }
            System.exit(1);
        }
    }

    /**
     * 注册 JVM 关闭钩子
     */
    public static void shutdownHook(ConfigurableApplicationContext finalContext) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LoggerUtil.debug("======================================");
            LoggerUtil.debug("检测到 JVM 关闭信号");
            LoggerUtil.debug("======================================");

            try {
                // 如果 Spring 容器还活跃，触发优雅关闭
                if (finalContext != null && finalContext.isActive()) {
                    LoggerUtil.debug("开始关闭 Spring 容器");
                    finalContext.close();
                    LoggerUtil.debug("Spring 容器已关闭");
                }
            } catch (Exception e) {
                LoggerUtil.error("关闭 Spring 容器时发生异常", e);
            }
        }, "jvm-shutdown-hook"));
    }
}

