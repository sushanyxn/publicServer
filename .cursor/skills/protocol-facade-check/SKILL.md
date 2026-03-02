---
name: protocol-facade-check
description: 检查 slg-net 下所有 packet 协议是否在 message.yml 中注册，以及是否在 slg-game 中有对应 Facade 接收；若缺失则按协议所在模块补充 message.yml 配置并在 slg-game 对应模块包下创建或扩展 Facade 进行接收处理。同时检查协议类是否有类注释与字段注释，缺失则一并补全（简体中文）。在用户要求检查协议配置、补全协议注册、补全 Facade 接收或统一协议与 Facade 时使用。
---

# 协议与 Facade 检查与补全

## 目标

1. 确保 `slg-net` 中所有 `packet` 包下的协议类均在 `message.yml` 中配置。
2. 确保需要由游戏服**接收**的协议在 `slg-game` 中有对应 Facade 的 `@MessageHandler` 方法处理。
3. 确保所有协议类具备**类注释**与必要的**字段注释**；若缺失则检查时一并补全（简体中文，符合项目 JavaDoc 规范）。
4. 若缺失：按协议定义所在模块在 `message.yml` 中补充；在 `slg-game` 对应模块包下创建或扩展 Facade 并实现接收逻辑；对协议类补充类注释与字段注释。

## 前置约定（见项目 .cursor/MEMORY.md）

- 协议定义路径：`slg-net/.../message/clientmessage/模块名/packet/` 与 `slg-net/.../message/innermessage/模块名/packet/`。
- 协议注册文件：`slg-net/src/main/resources/message.yml`，格式为按模块分组，每行 `协议号, 类名`。
- 协议号：0 保留；1–50 保留；100–999 为内部消息（按模块 10–20 个）；1000+ 为客户端消息（按模块约 100 个，如 login 1000–1099、scene 1100–1199、task 1200–1299）。
- Facade 位置：`slg-game/.../模块分类/模块名/facade/`，类名 `模块名Facade`，方法用 `@MessageHandler`，方法签名见下。
- 需要**接收**的协议：客户端请求 `CM_*`、内部消息 `IM_*`（除 RPC 由 slg-net 的 RpcFacade 处理外）。服务端推送 `SM_*` 与数据体 `*VO` 只需在 message.yml 中注册，不要求在 slg-game 中有接收 handler。

## 执行步骤

### 1. 收集所有协议类

- 扫描 `slg-net/src/main/java/com/slg/net/message/` 下所有 `**/packet/*.java`，得到协议类列表（含包名与所在模块）。
- 模块由 packet 的**上级目录名**决定：`clientmessage/模块名/packet/` 或 `innermessage/模块名/packet/`。

### 2. 检查 message.yml 配置

- 读取 `slg-net/src/main/resources/message.yml`，解析 `messages` 下各模块已注册的 `协议号, 类名`。
- 对每个协议类：若其**简单类名**未出现在任何模块的类名列表中，则视为**未在 message.yml 中配置**。
- 输出列表：未配置的协议类 → 所属模块（用于后续分配协议号与写入 message.yml）。

### 2.5 协议类注释检查与补全

- **范围**：对所有 `slg-net/.../message/` 下 `**/packet/*.java` 中的协议类执行。
- **类注释**（每个协议类必须包含）：
  - JavaDoc 格式，内容包含：类的简要描述（中文，说明协议用途、如「客户端登录请求」「场景节点出现推送」等）、`@author`、`@date`（格式 `yyyy-MM-dd`）。
  - 若类无类注释则添加完整类注释；若已有则补全缺失的 @author、@date 或描述；若描述与协议实际用途不符则按代码/命名修正。
- **字段注释**：
  - 对每个字段（含 Lombok 生成的 getter/setter 对应的字段）：若含义不直观、或为协议关键语义，补充 JavaDoc 或行内注释（简体中文），说明该字段在协议中的含义或取值说明。
  - 已有注释若与字段用途不符则修正；若字段名已自解释且无歧义可简短一行或省略。
- **规范**：注释使用简体中文；不写与签名完全重复的废话；不修改任何业务逻辑与代码结构，仅新增、补全或修正注释。
- 执行顺序：可与步骤 2 并行或在其后；补全注释后再继续步骤 3，以便新补充到 message.yml 的协议类也具备注释。

### 3. 分配协议号并补充 message.yml

- 对每个未配置的协议，根据其**模块**在 message.yml 中查找该模块已有最大协议号，在该模块下追加新行：`最大号+1, 类名`。
- 内部消息（innermessage）模块使用 100–999 段；客户端消息（clientmessage）模块使用 1000+ 段，且不跨模块占用（如 scene 仅用 1100–1199）。
- 若 message.yml 中尚不存在该模块，则新增模块块并分配该模块的起始协议号（参考 MEMORY 中的协议号分配规则）。
- 写回 `message.yml`，保持现有格式与注释。

### 4. 确定需要 Facade 接收的协议

- **需要**在 slg-game 有 handler 的：所有 `CM_*`（客户端请求）、以及 `innermessage` 中非 rpc 模块的 `IM_*`（如 socket 的 IM_RegisterSessionRequest/Responce）。RPC 的 IM_RpcRequest/IM_RpcRespone 由 slg-net 的 RpcFacade 处理，不在 slg-game 创建接收。
- **不需要**在 slg-game 有 handler 的：`SM_*`、`*VO`（仅需在 message.yml 注册）。

### 5. 检查 slg-game 中是否已有对应 Handler

- 在 `slg-game` 下搜索带 `@MessageHandler` 的方法，收集其第二个参数类型（协议类）。
- 对每个“需要接收”的协议类，若未出现在上述参数类型中，则视为**缺少 Facade 接收**。

### 6. 模块到 slg-game Facade 包路径映射

- `clientmessage/login` → `slg-game/.../base/login/facade/`，Facade 类名建议 `LoginFacade`。
- `clientmessage/scene` → `slg-game/.../scene/facade/`，Facade 类名建议 `SceneFacade`。
- `clientmessage/task` → `slg-game/.../develop/task/facade/`，Facade 类名建议 `TaskFacade`。
- `clientmessage/其他模块` → `slg-game/.../develop/模块名/facade/` 或 `base/模块名/facade/`（按业务性质选择）。
- `innermessage/socket` → `slg-game/.../net/facade/`，现有 `InnerSocketFacade`，在其上扩展方法即可。
- `innermessage/rpc` → 不在此技能中创建 slg-game Facade（由 slg-net RpcFacade 处理）。

### 7. 创建或扩展 Facade 并添加 @MessageHandler

- 若该模块在 slg-game 尚无对应 Facade 类：在映射的包路径下新建 `模块名Facade`，加 `@Component`，添加类注释（中文说明用途、@author、@date）。
- 若已有 Facade：在该类中新增方法。
- 方法规范：
  - 使用 `@MessageHandler`。
  - 登录/重连等**主体未绑定**的协议（如 `CM_LoginReq`、socket 注册）：两参 `(NetSession session, 协议类 message)`。
  - 其余**业务协议**（如 `CM_EnterScene`、`CM_GainTask`）：三参 `(NetSession session, 协议类 message, Player player)`。
- 方法体内可先委托给对应 Service（若尚无则只打日志或空实现，由用户后续补业务逻辑），避免只做转发不落地的空壳。

### 8. 收尾

- 再次确认：所有 packet 协议均在 message.yml 中；所有需要接收的 CM_/IM_（除 rpc）在 slg-game 均有对应 `@MessageHandler`；所有协议类均有类注释、关键字段有字段注释。
- 若项目有 `.cursorrules` 或 MEMORY 中的协议号范围说明，补充时不得突破各模块预留范围。

## 简要检查清单

- [ ] 扫描 slg-net 所有 packet 类，列出未在 message.yml 的项
- [ ] 检查所有协议类的类注释与字段注释，缺失或不符合规范的一并补全（简体中文）
- [ ] 为未配置项按模块分配协议号并写入 message.yml
- [ ] 列出需接收的 CM_/IM_（排除 rpc）且当前无 handler 的协议
- [ ] 按模块映射在 slg-game 创建或扩展 Facade，添加 @MessageHandler（两参/三参按规定）
- [ ] 协议号全局唯一、类名全局唯一；Facade 仅处理本模块协议
