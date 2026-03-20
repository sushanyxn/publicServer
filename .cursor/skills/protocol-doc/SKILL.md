---
name: protocol-scan
description: 扫描项目中所有协议类，生成协议文档。扫描 slg-net 下所有 packet 协议类，结合类注释、字段信息与 message.yml 按模块生成文档到 .cursor/protocol/ 目录。在用户要求扫描协议、生成协议文档、查看协议列表或更新协议文档时使用。
---

# 协议扫描与文档生成

## 目标

- 扫描 `slg-net` 下所有 packet 协议类。
- 提取每个协议的协议号、类名、类型、字段列表、JavaDoc 注释。
- 按模块生成协议文档，输出到 `.cursor/protocol/` 目录。

## 输入

- 无必传参数；可选指定模块名以缩小范围，未指定则处理全部协议。

## 协议来源与模块

- **客户端协议**：`slg-net/.../message/clientmessage/模块名/packet/`，模块即目录名（如 login、scene、task）。
- **内部协议**：`slg-net/.../message/innermessage/模块名/packet/`，模块即目录名（如 rpc、socket）。
- 协议号与类名以 `slg-net/src/main/resources/message.yml` 为准。
- **类型判断**：CM_ → 客户端请求，SM_ → 服务端推送，IM_ → 内部消息，*VO → 数据体(VO)，*Event → 内部事件，其他按前缀推断。

## 输出规范

- **目录**：`.cursor/protocol/`（若不存在则创建）。
- **主文件**：`.cursor/protocol/协议总览.md` — 全部协议的索引与速查表。
- **模块文件**：`.cursor/protocol/{模块名}.md` — 按 message.yml 中的模块划分（如 login.md、scene.md、hero.md）。

### 主文件格式

```markdown
# 协议总览

> 自动生成，请勿手动编辑。如需更新请重新执行 protocol-scan skill。
> 最后更新：{日期}

## 速查表

| 协议号 | 类名 | 类型 | 所属模块 | 说明 |
|--------|------|------|----------|------|
| 1001 | CM_LoginReq | 客户端请求 | 登录 | 登录请求消息 |
| 1002 | SM_LoginResp | 服务端推送 | 登录 | 登录响应消息 |
| ... | ... | ... | ... | ... |

## 模块索引

- [登录](login.md)
- [场景](scene.md)
- ...
```

### 模块文件格式

```markdown
# {模块中文名} 协议

> 协议号段：{起始}-{结束}
> 来源包：`{包路径}`

## 协议列表

### CM_LoginReq

- **协议号**：1001
- **类型**：客户端请求
- **说明**：登录请求消息，客户端发送给服务端的登录请求
- **字段**：
  - `account` (String) — 账号
  - `token` (String) — 登录令牌

### SM_LoginResp

- **协议号**：1002
- **类型**：服务端推送
- **说明**：登录响应消息
- **字段**：
  - `code` (int) — 返回码
  - `playerId` (long) — 玩家ID
```

## 执行步骤

### 1. 收集协议列表

- 读取 `slg-net/src/main/resources/message.yml`，建立 类名 → 协议号、模块 的映射。
- 扫描 `slg-net/src/main/java/com/slg/net/message/` 下所有 `**/packet/*.java`。
- 若指定了模块，则只保留该模块下的协议。

### 2. 解析每个协议类

对每个协议类：

- **读取类注释（JavaDoc）**：提取首段作为"说明"。
- **提取字段信息**：所有非静态字段的名称、类型，以及字段上的 JavaDoc/@comment 注释。
- **判断类型**：根据类名前缀判断（CM_=客户端请求，SM_=服务端推送，IM_=内部消息，*VO=数据体，*Event=内部事件）。
- 若类无注释：根据类名与模块推断说明，并注明"根据类名推断"。

### 3. 推断模块中文名

- 从 message.yml 的注释推断中文名（如 `# 登录模块 (1000-1099)` → 登录）。
- 常见映射：login→登录、scene→场景、task→任务、army→军团/部队、hero→英雄、notify→通知、gm→GM、report→战报、rpc→RPC、socket→SOCKET、event→事件。
- 若无法推断，使用模块目录名本身。

### 4. 生成文档

- 创建 `.cursor/protocol/` 目录（若不存在）。
- 按模块分别生成 `{模块名}.md` 文件，每个协议类一个 `###` 段落，包含协议号、类型、说明、字段列表。
- 生成 `协议总览.md` 索引文件，包含速查表和模块索引。

### 5. 收尾

- 确认 `.cursor/protocol/` 下文件已生成。
- 回复中给出文档路径与协议统计。

## 注意事项

- **只读操作**：不修改任何 Java 源代码或 message.yml，仅生成文档。
- **JavaDoc 优先**：协议说明优先使用 JavaDoc 注释；无注释时根据类名推断。
- **字段注释**：优先从 JavaDoc 的 `@comment` 或行内注释提取；无则仅列出类型与名称。
- **增量更新**：若 `.cursor/protocol/` 已有文件，覆盖更新（文档为自动生成，每次全量重写）。

## 检查清单

- [ ] 已读取 message.yml 获取全部协议号与模块映射
- [ ] 已扫描 slg-net 下全部 packet 类
- [ ] 每个协议均有类型、说明、字段列表
- [ ] 协议总览.md 包含完整速查表与模块索引
- [ ] 各模块文件包含详细协议信息
- [ ] 未修改任何 Java 源代码与 message.yml
