---
description: 合并启动模块（slg-singlestart）特有规范
globs:
  - "**/singlestart/**"
---

# 合并启动规范（slg-singlestart）

## 设计原则

- 将 `slg-game` 和 `slg-scene` 合并为同一进程运行，共享服务器 ID、RPC 服务和数据库
- **所有兼容逻辑封装在 `slg-singlestart` 内，尽量不修改 game/scene 的业务代码**

## Bean 冲突处理

- 同名 Bean 通过显式 `@Component` 命名区分（如 `gameSpringContext` / `sceneSpringContext`）
- 冲突的 Facade 和启动类通过 `SingleStartMain` 的 `excludeFilters` 排除

## 排除清单

| 排除的类 | 原因 |
|---------|------|
| `GameMain` / `SceneMain` | 避免注解重复导入 |
| `GameInnerRequestFacade` | @MessageHandler 与 Scene 端冲突 |
| `SceneInnerResponseFacade` | @MessageHandler 与 Game 端冲突 |
| `SceneClientMessageHandler` | 与 Game 端显式同名冲突 |

## 生命周期

- `SingleInitLifeCycle`（phase = SINGLE_INIT）：设置本地 Scene 的 ConnectState.READY + 批量初始化玩家场景
- 数据持久化统一在 `CacheFlushLifeCycle.stop()` 执行

## 路由

- `SingleRpcRouteService` 替代双进程路由，`isLocal()` 恒返回 true
