# AI 工作流说明

本文档说明 AI 助手在本仓库中的角色定位、可用 Skill 速查、以及常见多步工作流的推荐顺序。

---

## 一、角色定位

AI 助手在本仓库中承担以下职责：

- **按规范实现功能**：遵循 `.cursorrules` 与 `.cursor/MEMORY.md` 中的编码规范、命名约定、模块边界
- **补全协议与 Facade**：新增/修改协议时完成 message.yml 注册、Facade 创建、注释补全
- **框架测试与验证**：底层框架修改后执行测试计划、更新测试结果
- **代码质量维护**：补全 JavaDoc、重构过长方法/类、检查 RPC 断线安全性
- **配置校验**：检查表配置与 CSV 的一致性、协议注册完整性
- **分支管理**：按 master（框架/主干）与 thewartest（业务/测试）的分工进行跨分支同步

---

## 二、Skill 速查表

| Skill | 触发场景 | 说明 |
|-------|---------|------|
| **protocol-facade-check** | 检查协议配置、补全协议注册、补全 Facade 接收 | 扫描 slg-net 下所有 packet 协议，检查 message.yml 注册与 slg-game Facade 接收，缺失则补全 |
| **protocol-doc** | 生成协议说明、查看各协议作用 | 扫描协议类并结合 message.yml 生成说明文档，输出到 `.cursor/protocol` |
| **csv-config-check** | 检查表配置、校验 CSV、补全缺失 CSV | 传入配置类文件，检查对应 CSV 格式，处理 @TableRefCheck 关联表 |
| **javadoc** | 为包/类添加注释、补全 JavaDoc | 检查已有注释与代码一致性，不一致时以代码为准修改，使用简体中文 |
| **fix-method** | 根据注释补全方法、按 JavaDoc 实现方法 | 根据 JavaDoc 注释补全空/桩方法体，支持接口/抽象类在实现类中补全 |
| **refactor-structure** | 重构代码结构、拆分过长方法/类 | 保持逻辑不变，优化结构、可读性与可扩展性 |
| **framework-test** | 对框架进行测试、回归测试 | 读取/创建测试计划，执行测试，更新 `.cursor/tests/results` |
| **node-component-dev** | 新增/开发节点组件 | 基于 AbstractNodeComponent 开发，通过 ComponentEnum 注册，泛型限定节点类型 |
| **rpc-disconnect-check** | 检查 RPC 断线安全性、审查幂等性 | 扫描 @RpcMethod 及调用点，分析幂等性、状态一致性、异常处理 |
| **git-commit** | 提交代码、做 git 提交 | 按变更类型（框架层/业务层/cursor 工具）分次提交，生成规范 commit message |
| **branch-pick** | 从其他分支同步框架/业务/cursor 工具 | 根据 master 与 thewartest 的角色划分，合并或 cherry-pick 指定内容 |
| **ai-takeover-check** | 检查 AI 接管规范、规范性自检、审查项目规范完整性 | 检查文档一致性、规则文件、ADR、工作流、JavaDoc 覆盖等 9 个维度，生成诊断报告并修复 |

---

## 三、多步工作流示例

### 3.1 新增客户端协议（完整流程）

```
1. 在 slg-net/.../clientmessage/模块名/packet/ 下定义协议类（CM_/SM_ 前缀）
2. 在 slg-net/src/main/resources/message.yml 中注册协议号与类名
3. 在 slg-game/.../模块分类/模块名/facade/ 下创建或扩展 Facade
4. 使用 protocol-facade-check Skill 校验完整性
5. 使用 javadoc Skill 补全协议类与 Facade 的注释
```

### 3.2 新增表配置

```
1. 在业务模块的 table 包下编写 *Table 配置类，添加 @TableRefCheck 等注解
2. 在 table/ 目录下创建对应 CSV 文件
3. 使用 csv-config-check Skill 校验格式与关联表
```

### 3.3 框架修改与验证

```
1. 修改框架代码（slg-common/slg-net/slg-support 等）
2. 使用 framework-test Skill 执行对应框架的测试计划
3. 测试结果自动写入 .cursor/tests/results/
4. 若有失败用例，修复后重新执行
```

### 3.4 新增 RPC 接口

```
1. 在 slg-net/.../rpc/impl/模块名/ 下定义 RPC 接口，方法加 @RpcMethod
2. 在业务模块（slg-game 或 slg-scene）中实现接口
3. 使用 javadoc Skill 补全接口与实现类注释
4. 使用 rpc-disconnect-check Skill 检查断线安全性
```

### 3.5 新增场景节点组件

```
1. 使用 node-component-dev Skill 引导开发
2. 继承 AbstractNodeComponent，用泛型限定适用节点类型
3. 在 ComponentEnum 中注册新组件
4. 使用 javadoc Skill 补全注释
```

### 3.6 代码提交

```
1. 使用 git-commit Skill
2. Skill 自动按变更类型（框架层/业务层/cursor 工具）分次提交
3. 提交信息自动标注涉及范围
```

### 3.7 新增或修改 Skill

```
1. 在 .cursor/skills/{skill-name}/ 下创建或修改 SKILL.md
2. 更新 .cursor/docs/ai-workflow.md 的 Skill 速查表（新增/修改/删除条目）
3. 若 Skill 数量变化，更新评估文档中 Skill 计数引用
```

> 注意：新增、修改或删除 Skill 后，必须同步维护以上关联文件，保持一致。

---

*文档版本：1.1 | 最后更新：2026-03-06*
