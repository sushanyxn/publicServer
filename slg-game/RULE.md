# slg-game 模块开发规范

## 网络协议处理规范

### @MessageHandler 方法参数规范

在 `slg-game` 项目中接收客户端协议时，必须遵循以下参数定义规范：

#### 必须添加 Player 参数的场景

**除了登录和重连相关的网络协议外，所有其他客户端协议处理方法都必须添加 `Player` 参数作为第三个参数。**

原因：协议转发层会根据参数数量判断是否需要注入 Owner 对象。如果不添加 `Player` 参数，协议转发层无法正确调用方法。

#### 正确的参数定义

```java
// ✅ 正确：普通业务协议（必须包含 Player 参数）
@MessageHandler
public void gainTask(NetSession session, CM_GainTask req, Player player) {
    // 处理领取任务逻辑
}

// ✅ 正确：登录相关协议（不需要 Player 参数，因为玩家还未登录）
@MessageHandler
public void login(NetSession session, CM_LoginReq req) {
    // 处理登录逻辑
}

// ❌ 错误：普通业务协议缺少 Player 参数
@MessageHandler
public void gainTask(NetSession session, CM_GainTask req) {
    // 协议转发层无法正确调用此方法！
}
```

#### 参数顺序（必须严格遵守）

1. **第一个参数**：`NetSession session` - 网络会话对象
2. **第二个参数**：具体的协议消息对象（如 `CM_GainTask req`）
3. **第三个参数**：`Player player` - 玩家对象（由框架自动注入）

#### 例外情况

以下协议类型**不需要**添加 `Player` 参数：

1. **登录协议**：玩家还未登录，不存在 Player 对象
   - 例如：`CM_LoginReq`

2. **重连协议**：重新建立连接的协议
   - 例如：`CM_ReconnectReq`

3. **内部服务器通信协议**：服务器之间的通信协议
   - 例如：`IM_RegisterSessionRequest`

#### 技术原理

框架在处理消息时会检查方法参数数量：

```java
// 框架代码片段（MessageHandlerInjector.java）
boolean needOwner = paramTypes.length >= 3;
```

- 参数数量 < 3：不注入 Owner，适用于登录等特殊场景
- 参数数量 >= 3：自动注入 Owner（Player 对象）

#### 常见错误示例

```java
// ❌ 错误示例 1：手动获取 Player
@MessageHandler
public void gainTask(NetSession session, CM_GainTask req) {
    Player player = playerManager.getPlayer(session.getPlayerId());
    // 这个方法永远不会被调用，因为参数数量不正确！
}

// ❌ 错误示例 2：参数顺序错误
@MessageHandler
public void gainTask(Player player, NetSession session, CM_GainTask req) {
    // 第一个参数必须是 NetSession！
}

// ❌ 错误示例 3：登录协议错误地添加了 Player
@MessageHandler
public void login(NetSession session, CM_LoginReq req, Player player) {
    // 登录时玩家还不存在，这会导致框架错误！
}
```

#### 最佳实践

1. **业务协议**：始终添加 `Player` 参数
2. **登录协议**：只使用 `NetSession` 和协议对象
3. **参数命名**：使用清晰的参数名（`session`, `req`, `player`）
4. **不要手动获取 Player**：让框架自动注入，代码更简洁

---

## Facade 层规范

### Facade 类定义

```java
@Component
public class TaskFacade {
    
    @Autowired
    private TaskService taskService;
    
    @MessageHandler
    public void handleMessage(NetSession session, CM_Message req, Player player) {
        // 处理逻辑
    }
}
```

### 注意事项

1. Facade 类必须使用 `@Component` 注解
2. 处理方法必须使用 `@MessageHandler` 注解
3. 除登录/重连外，必须包含 `Player` 参数
4. Facade 只负责接收协议，具体业务逻辑委托给 Service 层

---

## 相关文档

- 网络协议定义：`slg-net/src/main/resources/message.yml`
- 协议处理框架：`slg-net/message/core/`
- 消息注入器：`MessageHandlerInjector.java`
