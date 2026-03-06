# ADR-003: 单链模块禁止 .join() 同步等待

## 状态

已采纳

## 背景

执行器分为多链（MultiExecutor，按 key 分链）和单链（SingleExecutor，所有任务共用一条串行链）。RPC 返回 `CompletableFuture`，虚拟线程下可通过 `.join()` 同步等待而不阻塞平台线程。但是否所有场景都适合 `.join()` 需区分。

## 决策

- **多链模块（PLAYER、PERSISTENCE、ROBOT、SCENE_NODE）允许 `.join()`**：只阻塞当前 key 的虚拟线程链，不影响其他 key
- **单链模块（SYSTEM、LOGIN、SCENE）禁止 `.join()`**：所有任务共用一条链，`.join()` 会阻塞整条链上的所有后续任务

## 理由

单链中 `.join()` 等待 RPC 返回时，该链上所有后续任务（如其他场景任务、其他登录请求）均排队等待，导致模块级阻塞。例如在 SCENE 链中等待会导致所有场景任务停滞。

## 后果

- 单链模块必须使用 `.whenComplete()` / `.thenApply()` 异步回调
- 回调在 IO 线程执行，需手动通过 `Executor.Xxx.execute(...)` 切回业务线程链
- 多链模块可直接 `.join()`，代码更直观、线程安全由 drain 串行保证
