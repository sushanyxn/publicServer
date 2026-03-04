package com.slg.singlestart;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.mongo.anno.EnableMongo;
import com.slg.net.zookeeper.anno.EnableZookeeper;
import com.slg.game.GameMain;
import com.slg.game.net.facade.GameInnerRequestFacade;
import com.slg.net.rpc.anno.EnableRpcRoute;
import com.slg.net.rpc.anno.EnableRpcServer;
import com.slg.net.socket.annotation.EnableWebSocketClient;
import com.slg.net.socket.annotation.EnableWebSocketServer;
import com.slg.scene.SceneMain;
import com.slg.scene.net.facade.SceneInnerResponseFacade;
import com.slg.scene.net.handler.SceneClientMessageHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 合并启动入口
 * 将 Game 和 Scene 合并到同一个进程中运行，共享同一个服务器ID、RPC服务和数据库
 *
 * @author yangxunan
 * @date 2026/02/24
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.slg.common",
                "com.slg.entity",
                "com.slg.table",
                "com.slg.game",
                "com.slg.scene",
                "com.slg.net.message",
                "com.slg.net.rpc",
                "com.slg.net.syncbus",
                "com.slg.net.crossevent",
                "com.slg.redis",
                "com.slg.singlestart"
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        GameMain.class,
                        SceneMain.class,
                        GameInnerRequestFacade.class,
                        SceneInnerResponseFacade.class,
                        SceneClientMessageHandler.class
                }
        )
)
@EnableWebSocketServer
@EnableWebSocketClient
@EnableRpcServer
@EnableRpcRoute          // 开启 Redis 跨服 RPC 转发
@EnableMongo             // 开启MongoDB数据库
@EnableZookeeper         // 开启Zookeeper配置读取和信息共享
public class SingleStartMain {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = null;
        try {
            LoggerUtil.info("SLG 合并服务器启动中...");
            context = SpringApplication.run(SingleStartMain.class, args);
            LoggerUtil.info("SLG 合并服务器启动成功！");
        } catch (Exception e) {
            LoggerUtil.error("SLG 合并服务器启动失败！", e);
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
