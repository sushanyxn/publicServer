package com.slg.net.thrift.config;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.config.WebSocketConnectionManagerConfiguration;
import com.slg.net.socket.config.WebSocketServerProperties;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import com.slg.net.socket.manager.WebSocketConnectionManager;
import com.slg.net.socket.server.WebSocketServer;
import com.slg.net.thrift.converter.IThriftConverter;
import com.slg.net.thrift.converter.ThriftConverterRegistry;
import com.slg.net.thrift.server.ThriftWebSocketChannelInitializer;
import org.apache.thrift.TBase;
import org.yaml.snakeyaml.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Thrift 协议适配层配置类
 * 通过 @EnableThriftAdapter 注解导入
 *
 * @author yangxunan
 * @date 2026/02/26
 */
@Configuration
@EnableConfigurationProperties(ThriftAdapterProperties.class)
@Import(WebSocketConnectionManagerConfiguration.class)
@ConditionalOnProperty(name = "thrift.adapter.enabled", havingValue = "true")
public class ThriftAdapterConfiguration {

    /**
     * 业务模块可选提供的手动转换器
     */
    @Autowired(required = false)
    private List<IThriftConverter<?, ?>> manualConverters;

    /**
     * Thrift 转换器注册中心
     * 从 thrift-mapping.yml 加载自动映射，并注册手动编写的转换器
     */
    @Bean
    public ThriftConverterRegistry thriftConverterRegistry() {
        ThriftConverterRegistry registry = new ThriftConverterRegistry();

        loadMappingConfig(registry);

        if (manualConverters != null) {
            for (IThriftConverter<?, ?> converter : manualConverters) {
                LoggerUtil.debug("注册手动 Thrift 转换器: {}", converter.getPojoType().getSimpleName());
            }
        }

        LoggerUtil.debug("Thrift 转换器注册中心初始化完成，共 {} 个转换器", registry.size());
        return registry;
    }

    /**
     * Thrift WebSocket 服务器
     */
    @Bean(name = "thriftServer")
    public WebSocketServer thriftServer(
            ThriftAdapterProperties thriftProperties,
            ThriftConverterRegistry converterRegistry,
            WebSocketConnectionManager connectionManager,
            @Qualifier("webSocketServerMessageHandler") WebSocketMessageHandler messageHandler) {

        WebSocketServerProperties wsProps = thriftProperties.toWebSocketServerProperties();

        ThriftWebSocketChannelInitializer channelInitializer = new ThriftWebSocketChannelInitializer(
                wsProps, messageHandler, connectionManager, converterRegistry, thriftProperties.getProtocol());

        LoggerUtil.debug("[Thrift Adapter] 创建 Thrift WebSocket 服务器，端口: {}", thriftProperties.getPort());
        return new WebSocketServer("Thrift", wsProps, connectionManager, messageHandler, channelInitializer);
    }

    /**
     * 从 thrift-mapping.yml 加载映射配置
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadMappingConfig(ThriftConverterRegistry registry) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("thrift-mapping.yml");
        if (inputStream == null) {
            LoggerUtil.debug("未找到 thrift-mapping.yml，跳过自动映射加载");
            return;
        }

        try {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);

            if (config == null || !config.containsKey("thrift-mappings")) {
                LoggerUtil.debug("thrift-mapping.yml 中无 thrift-mappings 配置");
                return;
            }

            Object mappingsObj = config.get("thrift-mappings");
            if (!(mappingsObj instanceof Map)) {
                return;
            }

            Map<String, List<String>> mappings = (Map<String, List<String>>) mappingsObj;

            for (Map.Entry<String, List<String>> moduleEntry : mappings.entrySet()) {
                List<String> lines = moduleEntry.getValue();
                if (lines == null) {
                    continue;
                }

                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length < 3) {
                        LoggerUtil.error("thrift-mapping.yml 格式错误: {}", line);
                        continue;
                    }

                    int thriftMsgId = Integer.parseInt(parts[0].trim());
                    String thriftClassName = parts[1].trim();
                    String pojoClassName = parts[2].trim();

                    try {
                        Class<? extends TBase> thriftClass =
                                (Class<? extends TBase>) Class.forName(thriftClassName);
                        Class<?> pojoClass = Class.forName(pojoClassName);

                        registry.registerAutoMapping(thriftMsgId, thriftClass, pojoClass);
                    } catch (ClassNotFoundException e) {
                        LoggerUtil.error("thrift-mapping.yml 类不存在: thrift={}, pojo={}",
                                thriftClassName, pojoClassName);
                    }
                }
            }

            LoggerUtil.debug("从 thrift-mapping.yml 加载了 {} 个映射", registry.size());
        } catch (Exception e) {
            LoggerUtil.error("加载 thrift-mapping.yml 失败", e);
        }
    }
}
