---
name: ai-takeover-check
description: 检查项目是否符合"AI 可完全接管开发"的规范性要求，覆盖文档一致性、规则文件、ADR、工作流说明、扩展指南、JavaDoc 覆盖等维度，对不符合项给出诊断并按用户确认后修复。在用户要求检查 AI 接管规范、审查项目规范完整性、或进行规范性自检时使用。
---

# AI 接管规范性检查与修复

## 目标

检查项目是否满足「AI 可完全接管开发」的规范性要求（设计原则参照 `.cursor/docs/AI完全接管开发的设计要点与项目评估.md`），输出诊断报告，按用户确认后修复不符合项。

## 检查维度与判定标准

### 1. 规范文件一致性

**检查内容**：

- `.cursorrules` 与 `.cursor/MEMORY.md` 之间无矛盾信息
- `.cursor/rules/` 下的规则文件与 MEMORY 之间无矛盾信息
- `.cursorrules` 保持精简索引角色（不超过 80 行），详细规范在 `.cursor/rules/` 中
- `.cursorrules` 顶部有"MEMORY 为事实来源"的分工声明

**检查方法**：

1. 读取 `.cursorrules`、`.cursor/MEMORY.md`、`.cursor/rules/*.md`
2. 提取以下关键信息进行交叉比对：
   - 技术栈版本（Java 版本、Spring Boot 版本）
   - 入口类名
   - 日志工具类名与级别规范
   - 执行器类型与使用方式
   - 协议命名规范
   - 模块层级与依赖规则
3. 任何矛盾标记为 **CONFLICT**，附具体位置与内容

**修复方式**：以 MEMORY 为准修正 `.cursorrules` 或 `rules/*.md`；若 MEMORY 本身有误（如类名错误）则先确认代码中的实际情况后修正 MEMORY。

---

### 2. 规则文件结构

**检查内容**：

- `.cursor/rules/` 目录是否存在
- 是否包含按主题拆分的规则文件（至少覆盖：通用、协议、执行器、日志、持久化）
- 每个规则文件是否有 YAML 前缀（`description`、`globs` 或 `alwaysApply`）
- `.cursorrules` 是否已精简为索引（非臃肿的大文件）

**判定**：

- `.cursor/rules/` 不存在或文件数 < 3 → **MISSING**
- 规则文件无 YAML 前缀 → **FORMAT_ERROR**
- `.cursorrules` 超过 80 行且包含大量详细规范 → **BLOATED**

**修复方式**：按设计要点文档 1.2 节"约定显式化"的原则，创建或补全按主题拆分的规则文件。

---

### 3. ADR（架构决策记录）

**检查内容**：

- `.cursor/docs/adr/` 目录是否存在
- 是否至少有 3 个 ADR 文件
- ADR 格式是否包含"背景→决策→后果"三部分

**判定**：

- 目录不存在 → **MISSING**
- 文件数 < 3 → **INSUFFICIENT**
- 文件不含三部分结构 → **FORMAT_ERROR**

**修复方式**：从 MEMORY 和技术选型文档中提取关键决策，生成 ADR 文件。优先提取：自研 RPC 选型、虚拟线程迁移、协议号分配规则等。

---

### 4. AI 工作流文档

**检查内容**：

- `.cursor/docs/ai-workflow.md` 是否存在
- 是否包含三部分：AI 角色定位、Skill 速查表、多步工作流示例
- Skill 速查表中的 Skill 数量是否与 `.cursor/skills/` 下实际 Skill 数量一致

**判定**：

- 文件不存在 → **MISSING**
- 缺少任一部分 → **INCOMPLETE**
- Skill 数量不一致 → **OUT_OF_SYNC**（可能有新增 Skill 未更新到速查表）

**修复方式**：创建或补全工作流文档；扫描 `.cursor/skills/*/SKILL.md` 获取所有 Skill 的 name 和 description，更新速查表。

---

### 5. 扩展指南

**检查内容**：

- `.cursor/docs/extension-guide.md` 是否存在
- 是否覆盖关键扩展场景（至少：协议+Facade、表配置+CSV、RPC 接口）

**判定**：

- 文件不存在 → **MISSING**
- 缺少关键场景 → **INCOMPLETE**

**修复方式**：创建或补全扩展指南，引用对应 Skill。

---

### 6. JavaDoc 类级覆盖

**检查内容**：

对以下四类文件检查是否有类级 JavaDoc（`/** ... */` 在 class/interface 声明之前）：
- 协议类：`slg-net/**/packet/*.java`
- 表配置类：`slg-game/**/table/*.java`、`slg-scene/**/table/*.java`
- Facade 类：`slg-game/**/facade/*.java`
- RPC 接口：`slg-net/**/rpc/impl/**/*.java`

同时检查类注释是否包含功能描述（不仅是 @author/@date）。

**判定**：

- 缺少类级 JavaDoc → **MISSING_JAVADOC**
- 仅有 @author/@date 无功能描述 → **MINIMAL_JAVADOC**

**修复方式**：使用 javadoc Skill 的规范为缺失文件补全类注释。

---

### 7. 文档规则合理性

**检查内容**：

- `.cursor/rules/general.md` 或 `.cursorrules` 中的文档规则是否允许在 `.cursor/docs/` 下创建架构与扩展文档
- 是否对根目录 README 做了限制（仅用户要求时修改）

**判定**：

- 规则中包含"禁止主动创建或编写文档文件"且无 `.cursor/docs/` 例外 → **TOO_RESTRICTIVE**

**修复方式**：修改为"允许在 `.cursor/docs/` 下创建或更新"。

---

### 8. 日志工具一致性

**检查内容**：

- MEMORY、rules/logging.md、代码中实际使用的日志类名是否统一
- 在 `slg-common` 中查找实际日志工具类名（`**/log/Logger*.java` 或 `**/log/Log*.java`）
- 与 MEMORY 和规则文件中声明的类名比对

**判定**：

- 文档中写的类名与代码中实际类名不一致 → **NAME_MISMATCH**

**修复方式**：以代码为准修正文档中的类名。

---

### 9. Skill 维护完整性

**检查内容**：

- 扫描 `.cursor/skills/*/SKILL.md`，获取所有 Skill 列表
- 检查 `.cursor/docs/ai-workflow.md` 的 Skill 速查表是否包含所有 Skill
- 检查 ai-workflow.md 中 Skill 数量是否与实际一致

**判定**：

- 速查表中缺少某 Skill → **NOT_IN_WORKFLOW**
- ai-workflow.md 中 Skill 数量描述过时 → **COUNT_OUTDATED**

**修复方式**：更新 ai-workflow.md 速查表。

---

## 执行步骤

### 1. 收集信息

并行读取以下文件：
- `.cursorrules`
- `.cursor/MEMORY.md`
- `.cursor/rules/*.md`（所有规则文件）
- `.cursor/docs/ai-workflow.md`
- `.cursor/docs/extension-guide.md`
- `.cursor/docs/adr/`（列出所有文件）
- `.cursor/docs/AI完全接管开发的设计要点与项目评估.md`

扫描以下文件是否有类级 JavaDoc：
- `slg-net/**/packet/*.java`
- `slg-game/**/table/*.java`、`slg-scene/**/table/*.java`
- `slg-game/**/facade/*.java`
- `slg-net/**/rpc/impl/**/*.java`

扫描 `.cursor/skills/*/SKILL.md` 获取所有 Skill 列表。

### 2. 逐维度检查并生成报告

按 9 个维度逐一检查，输出诊断报告格式：

```
## AI 接管规范性检查报告

### 检查结果总览

| # | 维度 | 状态 | 详情 |
|---|------|------|------|
| 1 | 规范文件一致性 | ✅ 通过 / ❌ CONFLICT | ... |
| 2 | 规则文件结构 | ✅ 通过 / ❌ MISSING | ... |
| ... | ... | ... | ... |

### 需修复项

（仅列出状态非 ✅ 的项，含具体位置与修复建议）
```

### 3. 确认与修复

- 将报告展示给用户
- 用户确认后，按修复方式逐项修正
- 修复完成后再次执行快速检查确认无遗留

### 4. 更新相关文档

- 若 JavaDoc 覆盖状态有变化，更新 MEMORY 中的 JavaDoc 覆盖状态
- 若 Skill 数量有变化，更新 ai-workflow.md 速查表

## 简要检查清单

- [ ] .cursorrules / MEMORY / rules 三方无矛盾
- [ ] `.cursor/rules/` 存在且含按主题拆分的规则文件（≥5 个），各有 YAML 前缀
- [ ] `.cursor/docs/adr/` 存在且含 ≥3 个格式合规的 ADR
- [ ] `.cursor/docs/ai-workflow.md` 存在且含角色定位、Skill 速查表、工作流示例
- [ ] `.cursor/docs/extension-guide.md` 存在且覆盖关键扩展场景
- [ ] 协议类/表配置类/Facade/RPC 接口全部有类级 JavaDoc（含功能描述）
- [ ] 文档规则允许在 `.cursor/docs/` 下主动创建文档
- [ ] 日志工具类名在文档与代码中一致
- [ ] Skill 速查表与实际 Skill 目录同步

## 触发场景

- 用户说"检查 AI 接管规范""审查项目规范完整性""规范性自检""检查项目是否符合 AI 接管要求"
- 项目进行了较大结构变更后的规范性回归检查
- 定期（如每周/每版本）执行的规范性审计
