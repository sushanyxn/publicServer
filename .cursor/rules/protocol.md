---
description: 网络协议定义、命名、message.yml 注册、Facade 处理规范
globs:
  - "**/message/**"
  - "**/facade/**"
  - "**/packet/**"
  - "**/message.yml"
---

# 协议规范

## 协议定义位置

- **客户端协议**: `slg-net/.../message/clientmessage/模块名/packet/`
- **内部协议**: `slg-net/.../message/innermessage/模块名/packet/`

## 命名规范

- 客户端请求：`CM_操作名`（如 `CM_LoginReq`）
- 服务端响应/推送：`SM_操作名`（如 `SM_LoginResp`）
- 内部消息：`IM_操作名`（如 `IM_RpcRequest`）
- VO/数据结构：以 `VO` 结尾（如 `TaskVO`）
- 所有协议类必须添加类注释

## 协议注册（message.yml）

- **路径**: `slg-net/src/main/resources/message.yml`
- 所有 `packet` 包下的协议类**必须**在 `message.yml` 中注册
- 协议号全局唯一，类名全局唯一，按模块分组
- **协议号分配**：
  - `0`: null（系统保留）
  - `1-50`: 基础类型（系统保留）
  - `100-999`: 内部消息，每模块预留 10-20 个
  - `1000+`: 客户端消息，每模块预留 100 个
- **格式**：`messages:` 下按模块分组，每行 `协议号, 类名`

## Facade 处理

- 位置：`slg-game/.../模块分类/模块名/facade/`
- 命名：`模块名Facade`（如 `LoginFacade`）
- 使用 `@Component` + `@MessageHandler` 注解
- **两参** `(NetSession, 协议类)`：仅用于未绑定主体阶段（登录/重连/内部协议/RPC）
- **三参** `(NetSession, 协议类, 操作主体)`：所有已绑定业务协议必须使用
  - 游戏服第三参为 `Player`，机器人为 `Robot`
  - 三参 handler 在玩家虚拟线程链中执行
- 一个 Facade 只处理本模块相关协议
