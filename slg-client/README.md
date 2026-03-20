# slg-client — 游戏客户端模拟器

基于 JavaFX + Spring Boot 的游戏客户端模拟器，用于快速验证客户端与服务端的交互逻辑。

## 核心功能

- **功能验证**：完全模拟客户端与服务器的 WebSocket 交互，验证业务功能正确性
- **AI 自动化测试**：通过 MCP（Model Context Protocol）协议暴露操作工具，供 Cursor AI 直接调用
- **多账号调试**：单端同时登录多个账号，每个账号拥有独立的 WebSocket 连接和数据上下文
- **协议同步扩展**：跟随服务端新增协议自动扩展对接

## 技术栈

| 项目 | 选型 |
|------|------|
| UI 框架 | JavaFX 21（OpenJFX） |
| 应用框架 | Spring Boot |
| 网络层 | 复用 slg-net WebSocket 客户端 |
| AI 接口 | Spring AI MCP Server（SSE 传输） |

## MCP 工具列表

slg-client 启动后在端口 `8099` 上以 SSE 方式提供以下 MCP 工具：

| 工具名 | 说明 |
|--------|------|
| `login` | 登录指定账号到游戏服务器 |
| `list_accounts` | 获取所有已登录的账号列表 |
| `get_account` | 获取指定账号的详细信息 |
| `disconnect` | 断开指定账号的连接 |
| `list_heroes` | 获取指定账号的英雄列表 |
| `hero_levelup` | 请求指定英雄升级 |
| `gm_command` | 发送 GM 指令到游戏服务器 |

## 启动方式

通过 `ClientMain.main()` 启动，会同时初始化 JavaFX 窗口和 Spring Boot 容器（含 MCP Server）。

## 配置项

```yaml
server:
  port: 8099                              # MCP Server 端口

client:
  server-url: ws://localhost:50001/ws     # 游戏服务器 WebSocket 地址
```

## 模块依赖

```
slg-common (一级)
  └── slg-net, slg-support (二级)
        └── slg-client (三级)
```

slg-client 依赖 slg-net 复用 WebSocket 客户端和协议编解码，依赖 slg-support 复用配置表框架。
