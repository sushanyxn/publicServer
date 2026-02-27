package com.slg.web;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.mysql.anno.EnableMysql;
import com.slg.net.rpc.anno.EnableRpcServer;
import com.slg.net.zookeeper.anno.EnableZookeeper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * SLG 导量服（Web 服务器）启动类
 * 负责客户端登录认证、game 服分配、账号管理、GM 后台管理
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.slg.entity",
        "com.slg.table",
        "com.slg.web",
        "com.slg.net.message",
        "com.slg.net.rpc",
        "com.slg.net.zookeeper",
        "com.slg.redis",
        "com.slg.common.executor"
})
@EnableRpcServer
@EnableMysql
@EnableZookeeper
public class WebMain extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(WebMain.class);
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = null;
        try {
            LoggerUtil.info("SLG 导量服启动中...");
            context = SpringApplication.run(WebMain.class, args);
            LoggerUtil.info("SLG 导量服启动成功！");
        } catch (Exception e) {
            LoggerUtil.error("SLG 导量服启动失败！", e);
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
