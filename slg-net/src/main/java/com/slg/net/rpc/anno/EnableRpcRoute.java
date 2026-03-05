package com.slg.net.rpc.anno;

import com.slg.net.enable.RpcRouteMarkerConfiguration;
import com.slg.net.rpc.route.config.RpcRouteConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 RPC Redis 路由转发
 * 在 Spring Boot 主类上添加此注解以启用 Redis Stream 跨服 RPC 转发功能
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableRpcRoute
 * public class GameMain {
 *     public static void main(String[] args) {
 *         SpringApplication.run(GameMain.class, args);
 *     }
 * }
 * }
 * </pre>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * rpc:
 *   route:
 *     redis:
 *       host: localhost          # 转发 Redis 主机
 *       port: 6380               # 转发 Redis 端口（区别于业务 Redis 6379）
 *       consumer-group: rpc-route-group
 *       batch-size: 10
 *       block-seconds: 1
 * </pre>
 *
 * <p>前置条件：
 * <ul>
 *   <li>路由服务实现类需同时实现 {@link com.slg.net.rpc.route.IRouteSupportService} 和
 *       {@link com.slg.net.rpc.route.IRpcRouteSupportService}</li>
 *   <li>需要配置 docker/redis-route 中的转发 Redis 并启动</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/03/04
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({RpcRouteMarkerConfiguration.class, RpcRouteConfiguration.class})
public @interface EnableRpcRoute {
}
