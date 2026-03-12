package com.slg.game;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.mongo.anno.EnableMongo;
import com.slg.net.rpc.anno.EnableRpcRoute;
import com.slg.net.rpc.anno.EnableRpcServer;
import com.slg.net.socket.annotation.EnableWebSocketClient;
import com.slg.net.socket.annotation.EnableWebSocketServer;
import com.slg.net.zookeeper.anno.EnableZookeeper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * SLG 游戏服务器启动类
 *
 * @author yangxunan
 * @date 2025-12-23
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.slg.common",   // 通用工具
        "com.slg.entity",   // 业务缓存，数据库对接框架(可接入不同的数据库)，mongo数据库
        "com.slg.table",    // 配置表框架
        "com.slg.sharedmodules",  // 共享模块（进度、战斗等）
        "com.slg.game",     // 游戏业务逻辑
        "com.slg.net.message",   // 协议解析
        "com.slg.net.rpc",  // rpc框架
        "com.slg.net.syncbus",  // 同步总线框架
        "com.slg.net.crossevent", // 跨服事件框架
        "com.slg.redis"          // Redis 缓存框架
})
@EnableWebSocketServer   // 开启ws服务端
@EnableWebSocketClient   // 开启ws客户端
@EnableRpcServer         // 开启rpc服务端
@EnableRpcRoute          // 开启 Redis 跨服 RPC 转发
@EnableMongo             // 开启MongoDB数据库
@EnableZookeeper         // 开启Zookeeper配置读取和信息共享
public class GameMain {

    /**
     * 应用程序入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args){
        ConfigurableApplicationContext context = null;

        try {
            LoggerUtil.info("SLG 游戏服务器启动中...");

            // 启动 Spring Boot 应用
            context = SpringApplication.run(GameMain.class, args);

            LoggerUtil.info("SLG 游戏服务器启动成功！");

        } catch (Exception e) {
            LoggerUtil.error("SLG 游戏服务器启动失败！", e);
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
}
