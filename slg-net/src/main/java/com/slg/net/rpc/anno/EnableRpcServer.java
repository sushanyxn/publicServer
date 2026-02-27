package com.slg.net.rpc.anno;

import com.slg.net.rpc.config.RpcServerConfiguration;
import com.slg.net.rpc.config.RpcServerLifeCycleConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 RPC 服务端
 * 在 Spring Boot 主类上添加此注解以启用 RPC 服务端功能
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableRpcServer
 * public class SceneApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(SceneApplication.class, args);
 *     }
 * }
 * }
 * </pre>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * rpc:
 *   server:
 *     port: 8081           # RPC 服务器端口
 *     path: /rpc          # WebSocket 路径
 *     boss-threads: 1     # Boss 线程数
 *     worker-threads: 4   # Worker 线程数
 *     all-idle-time: 300  # 空闲超时（秒）
 * </pre>
 *
 * @author yangxunan
 * @date 2026/01/26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
    RpcServerConfiguration.class,
    RpcServerLifeCycleConfiguration.class
})
public @interface EnableRpcServer {
}

