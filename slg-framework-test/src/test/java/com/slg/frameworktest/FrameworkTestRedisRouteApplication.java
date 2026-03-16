package com.slg.frameworktest;

import com.slg.net.rpc.anno.EnableRpcRoute;
import com.slg.net.rpc.config.RpcClientConfiguration;
import com.slg.net.rpc.route.config.RpcRouteConfiguration;
import com.slg.sharedmodules.attribute.manager.AttributeManager;
import com.slg.sharedmodules.progress.manager.ProgressManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Redis Route 集成测试用入口，启用 @EnableRpcRoute，使用 Testcontainers Redis 作为转发 Redis
 * 仅扫描 com.slg.net.rpc（避免引入 WebSocket/CrossEvent 等不需要的组件）
 *
 * <p>注意：需排除 {@link RpcRouteConfiguration}，因为 ComponentScan 先于 @Import 处理，
 * 会导致 @ConditionalOnBean(RpcRouteEnabledMarker) 评估时标记 bean 尚不存在；
 * 排除后由 @EnableRpcRoute 的 @Import 独立加载，保证标记先于条件评估。
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
    "com.slg.sharedmodules",
    "com.slg.redis",
    "com.slg.net.rpc",
    "com.slg.frameworktest"
}, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {
        ProgressManager.class,
        AttributeManager.class,
        RpcRouteConfiguration.class,
        FrameworkTestApplication.class,
        FrameworkTestRedisOnlyApplication.class,
        RpcClientConfiguration.class
    }
))
public class FrameworkTestRedisRouteApplication {
}
