package com.slg.client.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 工具注册配置
 * 将 @Tool 注解的方法注册为 MCP 工具，供 Cursor AI 通过 SSE 协议调用
 *
 * @author yangxunan
 * @date 2026/03/20
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider clientToolCallbackProvider(ClientMcpTools clientMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(clientMcpTools)
                .build();
    }
}
