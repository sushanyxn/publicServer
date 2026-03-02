---
name: protocol-doc
description: 生成协议文件说明，描述每个协议的作用。扫描 slg-net 下所有 packet 协议类，结合类注释与 message.yml 生成说明文档，输出到 .cursor/protocol 目录。在用户要求生成协议说明、协议文档或查看各协议作用时使用。
---

# 生成协议文件说明

## 目标

- 扫描项目中所有协议类（`slg-net/.../message/.../packet/*.java`）。
- 为每个协议生成**作用描述**（优先使用协议类已有的 JavaDoc/类注释，无则根据类名与模块推断）。
- 将说明文档输出到 **`.cursor/protocol`** 目录。

## 输入

- 无必传参数；可选指定模块名（如只生成 `login`、`scene`）以缩小范围，未指定则处理全部协议。

## 协议来源与模块

- **客户端协议**：`slg-net/.../message/clientmessage/模块名/packet/`，模块即目录名（如 login、scene、task）。
- **内部协议**：`slg-net/.../message/innermessage/模块名/packet/`，模块即目录名（如 rpc、socket）。
- 协议号与类名以 `slg-net/src/main/resources/message.yml` 为准，生成说明时一并列出协议号。

## 输出规范

- **目录**：`.cursor/protocol/`（若不存在则创建）。
- **主文件**：`.cursor/protocol/协议说明.md`（或按模块拆分为多个文件，见下）。
  - 若协议数量较多，可按模块拆分，例如：`.cursor/protocol/login.md`、`.cursor/protocol/scene.md`、`.cursor/protocol/innermessage.md` 等，主文件可保留总览与索引。
- **内容格式**（每个协议至少包含）：
  - **协议类名**
  - **协议号**（从 message.yml 获取，若未配置则标注“未配置”）
  - **所属模块**
  - **方向/类型**：客户端请求(CM_)、服务端推送(SM_)、数据体(VO)、内部消息(IM_)等
  - **作用描述**：一句话或简短段落，说明该协议的用途、使用场景（优先来自类注释，无则推断）

## 执行步骤

### 1. 收集协议列表

- 扫描 `slg-net/src/main/java/com/slg/net/message/` 下所有 `**/packet/*.java`。
- 读取 `message.yml`，建立 类名 → 协议号、模块 的映射。
- 若指定了模块，则只保留该模块下的协议。

### 2. 获取每个协议的作用描述

- 对每个协议类：读取其**类注释（JavaDoc）**，提取首段或描述性句子作为“作用描述”。
- 若类无注释或注释为空：根据类名前缀与模块推断（如 CM_ 多为请求、SM_ 多为推送、IM_ 为内部消息），并注明“根据类名推断”。

### 3. 生成并写入文档

- 按模块分组，协议号升序或按 message.yml 顺序排列。
- 使用 Markdown 表格或列表，列出：协议号、类名、模块、方向/类型、作用描述。
- 写入 `.cursor/protocol/` 下主文件或分模块文件；若目录不存在则先创建。

### 4. 收尾

- 确认 `.cursor/protocol/` 下文件已生成，可在回复中给出文档路径与简要说明。

## 文档示例结构

```markdown
# 协议说明

## 登录模块 (login)

| 协议号 | 类名 | 类型 | 作用 |
|--------|------|------|------|
| 1001 | CM_LoginReq | 客户端请求 | 登录请求，携带账号等信息 |
| 1002 | SM_LoginResp | 服务端推送 | 登录结果响应 |

## 场景模块 (scene)
...
```

## 简要检查清单

- [ ] 已扫描 slg-net 下全部 packet 类，并读取 message.yml 获取协议号与模块
- [ ] 每个协议均有作用描述（类注释或推断）
- [ ] 文档已写入 .cursor/protocol/ 目录，格式清晰可读
- [ ] 未修改任何协议代码与 message.yml，仅生成说明文档
