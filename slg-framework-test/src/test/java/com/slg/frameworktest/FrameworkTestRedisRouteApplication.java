package com.slg.frameworktest;

import com.slg.common.progress.manager.ProgressManager;
import com.slg.net.rpc.anno.EnableRpcRoute;
import com.slg.net.rpc.config.RpcClientConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Redis Route 集成测试用入口，启用 @EnableRpcRoute，使用 Testcontainers Redis 作为转发 Redis
 * 仅扫描 com.slg.net.rpc（避免引入 WebSocket/CrossEvent 等不需要的组件）
 *
 * @author framework-test
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableRpcRoute
@ComponentScan(basePackages = {
    "com.slg.common",
    "com.slg.redis",
    "com.slg.net.rpc",
    "com.slg.frameworktest"
}, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {
        ProgressManager.class,
        FrameworkTestApplication.class,
        FrameworkTestRedisOnlyApplication.class,
        RpcClientConfiguration.class
    }
))
public class FrameworkTestRedisRouteApplication {
}
