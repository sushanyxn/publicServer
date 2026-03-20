---
name: client-module-dev
description: 为 slg-client 模拟客户端开发新的业务模块。根据用户指定的模块名称和对应的服务端协议，生成 ClientHandler（协议处理）、IClientModule 面板（UI 展示）和 MCP 工具方法。遵循客户端模拟器的架构约定和命名规范。在用户要求为模拟客户端新增模块、对接新协议、开发新面板或扩展客户端功能时使用。
---

# 为模拟客户端开发业务模块

## 输入

- **模块名称**：业务模块名（如 `hero`、`city`、`alliance`、`activity`）。
- **协议范围**（可选）：该模块涉及的 `SM_` 协议类列表；若未提供则自动从 `slg-net/.../clientmessage/` 下扫描对应模块包。
- **功能描述**（可选）：用户对模块 UI 和交互的具体要求。

## 前置知识

执行前必须阅读以下文件获取项目约定：

1. **`.cursor/docs/模拟客户端文档.md`** — 客户端架构、目录结构、命名约定、开发约束
2. **`.cursor/MEMORY.md`** — 项目编码规范、协议命名规则、Lombok 使用规范
3. **`slg-client/src/main/java/com/slg/client/core/module/IClientModule.java`** — 模块接口定义
4. **`slg-client/src/main/java/com/slg/client/core/account/ClientAccount.java`** — 账号上下文结构

可选参考（按需阅读）：

- `slg-client/src/main/java/com/slg/client/message/login/LoginClientHandler.java` — ClientHandler 示例
- `slg-client/src/main/java/com/slg/client/ui/panel/PlayerInfoPanel.java` — IClientModule 面板示例
- `slg-client/src/main/java/com/slg/client/ui/panel/MapPanel.java` — 大地图面板示例
- `slg-client/src/main/java/com/slg/client/mcp/ClientMcpTools.java` — MCP 工具示例

## 每个模块需要生成的文件

以模块名 `hero` 为例：

| 文件 | 位置 | 说明 |
|------|------|------|
| `HeroClientHandler.java` | `message/hero/` | 处理 `SM_Hero*` 协议，更新 ClientAccount 数据 |
| `HeroPanel.java` | `ui/panel/` | 实现 `IClientModule`，提供英雄模块 UI |
| 模块数据类（可选） | `core/account/data/` 或内嵌 | 存储在 `ClientAccount.moduleDataMap` 中 |
| MCP 工具（可选） | `mcp/ClientMcpTools.java` | 新增 `@McpTool` 方法供 AI 调用 |

## 命名规范

| 类别 | 规则 | 示例 |
|------|------|------|
| 协议处理类 | `{模块名}ClientHandler` | `HeroClientHandler`、`CityClientHandler` |
| 模块面板类 | `{模块名}Panel` | `HeroPanel`、`CityPanel` |
| 协议处理包 | `com.slg.client.message.{模块名}` | `com.slg.client.message.hero` |
| 处理方法注解 | `@MessageHandler` | — |
| 方法签名 | `(NetSession, SM_Xxx, ClientAccount)` | 三参形式，第三参为 `ClientAccount` |

## 开发步骤

### 1. 识别协议

- 查找 `slg-net/src/main/java/com/slg/net/message/clientmessage/{模块名}/packet/` 下所有 `SM_` 协议类
- 同时确认对应的 `CM_` 请求协议（面板中需要发送）
- 若目标模块下暂无协议定义，先提示用户并暂停

### 2. 创建 ClientHandler

在 `slg-client/src/main/java/com/slg/client/message/{模块名}/` 下创建 `{模块名}ClientHandler.java`：

```java
package com.slg.client.message.{模块名};

// ...imports...

/**
 * 客户端{模块中文名}消息处理
 *
 * @author yangxunan
 * @date {当前日期}
 */
@Component
public class {模块名}ClientHandler {

    @MessageHandler
    public void onXxx(NetSession session, SM_XxxResp message, ClientAccount account) {
        // 1. 更新 account 数据（存入 moduleDataMap）
        // 2. Platform.runLater() 刷新 UI（若有必要）
    }
}
```

**要点**：
- 标注 `@Component`
- 每个 `SM_` 协议对应一个 `@MessageHandler` 方法
- 方法内先更新数据，再通过 `Platform.runLater()` 刷新 UI
- 使用 `account.setModuleData(moduleName, data)` 存储模块数据
- 使用 `LoggerUtil` 记录关键日志

### 3. 创建模块面板

在 `slg-client/src/main/java/com/slg/client/ui/panel/` 下创建 `{模块名}Panel.java`：

```java
package com.slg.client.ui.panel;

// ...imports...

/**
 * {模块中文名}模块面板
 *
 * @author yangxunan
 * @date {当前日期}
 */
@Component
public class {模块名}Panel implements IClientModule {

    @Override
    public String moduleName() {
        return "{模块中文名}";
    }

    @Override
    public int order() {
        return {排序值};  // 角色信息=1, 大地图=50, 业务模块建议 10~40
    }

    @Override
    public Pane createPanel(ClientAccount account) {
        // 1. 从 account.getModuleData() 获取数据
        // 2. 构建 JavaFX 控件展示数据
        // 3. 添加交互控件（按钮等），点击时调用 account.sendMessage(new CM_Xxx())
        // 4. 监听 account 的可观察属性实现数据绑定
    }
}
```

**要点**：
- 标注 `@Component`，实现 `IClientModule`
- `createPanel()` 接收 `ClientAccount`，所有数据和操作都基于该账号
- 不使用美术资源，使用 JavaFX 原生控件 + CSS 样式类
- 交互操作通过 `account.sendMessage()` 发送协议
- 可在 `css/client.css` 中追加样式

### 4. 创建模块数据类（可选）

若模块数据较复杂，创建独立的数据类：

```java
@Getter
@Setter
public class HeroModuleData {
    private List<HeroVO> heroList = new ArrayList<>();
    // ...
}
```

存储：`account.setModuleData("hero", heroModuleData)`
读取：`account.getModuleData("hero", HeroModuleData.class)`

### 5. 扩展 MCP 工具（可选）

若需要 AI 操作该模块，在 `mcp/ClientMcpTools.java` 中新增 `@Tool` 方法：

```java
@Tool(name = "list_heroes", description = "获取指定账号的英雄列表")
public String listHeroes(
        @ToolParam(description = "账号名") String account) {
    ClientAccount clientAccount = accountManager.getByAccountName(account);
    if (clientAccount == null) {
        return "账号不存在: " + account;
    }
    // 从 clientAccount.getModuleData() 读取数据并返回
}
```

**要点**：
- 所有 MCP 工具方法集中在 `ClientMcpTools` 中
- 返回值为 `String`，内容清晰易读
- 参数使用 `@ToolParam` 注解描述（`org.springframework.ai.tool.annotation`），框架自动生成 JSON Schema

### 6. 验证

- 确认 ClientHandler 的 `@MessageHandler` 方法签名正确（三参 / 两参）
- 确认面板类标注了 `@Component` 且实现了 `IClientModule`
- 确认新文件在正确的包路径下
- 运行 Maven 编译验证：`mvn compile -pl slg-client -am -q`

## 检查清单

- [ ] 已阅读模拟客户端文档和 MEMORY.md
- [ ] 已在 `slg-net` 中确认目标模块的 SM_/CM_ 协议
- [ ] ClientHandler 类名为 `{模块名}ClientHandler`，在 `message/{模块名}/` 包下
- [ ] ClientHandler 标注 `@Component`，方法标注 `@MessageHandler`
- [ ] 面板类实现 `IClientModule`，标注 `@Component`
- [ ] 面板 `createPanel()` 使用 `ClientAccount` 参数，不依赖全局状态
- [ ] 数据存储在 `ClientAccount.moduleDataMap` 中
- [ ] UI 更新使用 `Platform.runLater()`
- [ ] 不使用美术资源
- [ ] 使用 `LoggerUtil` 记录日志
- [ ] Maven 编译通过
