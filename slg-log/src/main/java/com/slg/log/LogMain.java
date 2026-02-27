package com.slg.log;

import com.slg.entity.mysql.anno.EnableMysql;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 告警日志分析系统启动类
 * 独立部署的 Web 服务，提供日志搜索、统计分析和用户管理功能
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableMysql
@ComponentScan(basePackages = {
        "com.slg.entity",
        "com.slg.log",
        "com.slg.common.executor"
})
public class LogMain {

    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext context = SpringApplication.run(LogMain.class, args);
            Environment env = context.getEnvironment();
            String port = env.getProperty("server.port", "8080");
            log.info("""
                    
                    =====================================================
                      SLG Log Server Started Successfully!
                    =====================================================
                      Port:    {}
                      URL:     http://localhost:{}
                    =====================================================
                    """, port, port);
        } catch (Exception e) {
            log.error("SLG 日志服启动失败！", e);
            System.exit(1);
        }
    }
}
