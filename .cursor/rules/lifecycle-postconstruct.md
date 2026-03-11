---
description: 禁止在 PostConstruct 中做数据库/RPC，数据与业务初始化应放在对应 lifecycle
alwaysApply: true
---

# PostConstruct 与生命周期分工

## 禁止行为

- **禁止**在 `@PostConstruct` 方法中执行：
  - 读取数据库（含 Mongo、MySQL、Redis 等）
  - RPC 调用（含同步、异步调用其他进程）
- 原因：Bean 构造阶段尚未进入有序启动流程，依赖的数据或远程服务可能未就绪，易导致启动顺序混乱或失败。

## 正确做法

| 需求类型 | 放置位置 | 说明 |
|----------|----------|------|
| **数据加载** | 数据加载的 Lifecycle | 实现 `SmartLifecycle`，在 `start()` 中执行 DB 加载；`getPhase()` 使用 `LifecyclePhase.DATA_LOADING`。例如 game 服：`GameDataLoadingLifeCycle`（预加载账号、玩家等） |
| **业务初始化** | 业务 Lifecycle | 实现 `SmartLifecycle`，在 `start()` 中做连接建立、RPC 注册、业务组件初始化等。例如：`GameInitLifeCycle`（game 服）、`SceneInitLifeCycle`（场景服）、`CacheFlushLifeCycle`（缓存刷盘） |

## 示例

```java
// ❌ 禁止：在 PostConstruct 中读库或 RPC
@PostConstruct
public void init() {
    playerRepository.findAll();  // 读库
    sceneRpcService.getNodes();  // RPC
}

// ✅ 数据加载：在数据加载 Lifecycle 的 start() 中
@Override
public void start() {
    accountManager.loadAll();
    playerManager.loadPlayers();
}

// ✅ 业务初始化：在业务 Lifecycle 的 start() 中
@Override
public void start() {
    InnerSessionManager.getInstance().startServerConnection(...);
    shareService.createInstance(...);
}
```

## 参考

- 阶段常量：`com.slg.common.constant.LifecyclePhase`（如 `DATA_LOADING`）
- game 服：`GameDataLoadingLifeCycle`、`GameInitLifeCycle`
- scene 服：`SceneInitLifeCycle`
