package com.slg.net.thrift.anno;

import com.slg.net.thrift.config.ThriftAdapterConfiguration;
import com.slg.net.thrift.config.ThriftAdapterLifeCycleConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 Thrift 协议适配层
 * 在 Spring Boot 主类上添加此注解以启用 Thrift 客户端协议转换
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * thrift:
 *   adapter:
 *     enabled: true       # 启用 Thrift 适配
 *     port: 50002         # Thrift WebSocket 端口
 *     path: /ws           # WebSocket 路径
 *     protocol: binary    # Thrift 协议: binary / compact
 * </pre>
 *
 * @author yangxunan
 * @date 2026/02/26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
    ThriftAdapterConfiguration.class,
    ThriftAdapterLifeCycleConfiguration.class
})
public @interface EnableThriftAdapter {
}
