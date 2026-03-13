package com.slg.client;

import com.slg.net.socket.annotation.EnableWebSocketClient;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 客户端模拟器启动类
 * 通过 JavaFX Application.launch 启动，Spring Boot 在 JavaFX init 阶段初始化
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.slg.common",
    "com.slg.client",
    "com.slg.net.message",
})
@EnableWebSocketClient
public class ClientMain {

    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
