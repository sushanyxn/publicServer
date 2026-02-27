package com.slg.net.socket.annotation;

import com.slg.net.socket.client.WebSocketClientManager;
import com.slg.net.socket.config.WebSocketClientConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 WebSocket 客户端
 * 在 Spring Boot 主类上添加此注解以启用 WebSocket 客户端功能
 *
 * @author yangxunan
 * @date 2025-12-25
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({WebSocketClientConfiguration.class, WebSocketClientManager.class})
public @interface EnableWebSocketClient {
}
