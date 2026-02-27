package com.slg.net.socket.annotation;

import com.slg.net.socket.config.WebSocketServerConfiguration;
import com.slg.net.socket.config.WebSocketServerLifeCycleConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 WebSocket 服务端
 * 在 Spring Boot 主类上添加此注解以启用 WebSocket 服务端功能
 *
 * @author yangxunan
 * @date 2025-12-25
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
    WebSocketServerConfiguration.class,
    WebSocketServerLifeCycleConfiguration.class
})
public @interface EnableWebSocketServer {
}
