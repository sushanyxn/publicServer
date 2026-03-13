package com.slg.client;

import com.slg.client.ui.MainWindow;
import com.slg.common.log.LoggerUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX Application 入口
 * 负责 Spring Boot 容器与 JavaFX 生命周期的桥接
 *
 * @author yangxunan
 * @date 2026/03/13
 */
public class ClientApp extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        LoggerUtil.info("客户端模拟器启动中...");
        springContext = SpringApplication.run(ClientMain.class);
    }

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = springContext.getBean(MainWindow.class);
        mainWindow.show(primaryStage);
        LoggerUtil.info("客户端模拟器启动完成");
    }

    @Override
    public void stop() {
        LoggerUtil.info("客户端模拟器关闭中...");
        if (springContext != null && springContext.isActive()) {
            springContext.close();
        }
        Platform.exit();
    }
}
