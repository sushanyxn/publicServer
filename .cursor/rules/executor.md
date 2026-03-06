---
description: 执行器类型、虚拟线程、定时任务与 RPC 线程分派规范
globs:
  - "**/executor/**"
  - "**/task/**"
---

# 执行器与虚拟线程规范

## 统一执行器

- 项目使用**虚拟线程**统一替代传统线程池，底层基于 `KeyedVirtualExecutor` + `GlobalScheduler`
- **禁止**在业务代码中自行创建 `ExecutorService`、`ScheduledExecutorService`、`ThreadPoolExecutor`
- **禁止**使用 `Executors.newFixedThreadPool()` 等工厂方法
- 所有异步任务通过 `com.slg.common.executor.Executor` 的静态字段调用

## 执行器类型

| 类型 | 说明 |
|------|------|
| `MultiExecutor` | 多链执行器，按 key 分链，同 key 串行、不同 key 并发，必须传 key |
| `SingleExecutor` | 单链执行器，该模块所有任务共用一条串行链，不接受 key |

## 模块与字段

| TaskModule | 链类型 | Executor 字段 | 说明 |
|------------|--------|---------------|------|
| PLAYER | 多链 | `Executor.Player` | 按 playerId 分链 |
| SCENE_NODE | 多链 | `Executor.SceneNode` | 按 nodeId 分链 |
| PERSISTENCE | 多链 | `Executor.Persistence` | 按实体 ID 分链 |
| ROBOT | 多链 | `Executor.Robot` | 按 robotId 分链 |
| SYSTEM | 单链 | `Executor.System` | 所有系统任务串行 |
| LOGIN | 单链 | `Executor.Login` | 所有登录任务串行 |
| SCENE | 单链 | `Executor.Scene` | 所有场景任务串行 |

## 关键约束

- 虚拟线程中**禁止** `synchronized`（会 pin 载体线程），应使用 `ReentrantLock`
- **单链模块禁止** `.join()` 同步等待 RPC（会阻塞整条链），必须用异步回调 + 手动分派
- **多链模块可以** `.join()` 同步等待（只阻塞当前 key）
- 新增 `TaskModule` 后在 `Executor` 类中添加对应类型的 `public static` 字段即可

## 定时任务

- 使用 `Executor.Xxx.schedule(...)` / `scheduleWithFixedDelay(...)` / `scheduleAtFixedRate(...)`
- "完成后再延迟"语义使用自递归调度模式

## RPC 线程分派

- `@RpcMethod(useModule = TaskModule.XXX)` 指定执行模块
- RPC 分发通过 `KeyedVirtualExecutor` 直达，无中间接口层
