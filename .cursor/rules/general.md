---
description: 通用编码规范、AI 使用规则、安全与配置、测试与构建要求
alwaysApply: true
---

# 通用规则

> `.cursor/MEMORY.md` 是架构与流程的详细事实来源；`.cursorrules` 是编码规范摘要，两者冲突时以 MEMORY 为准。

## 技术栈

- **语言**: Java 21
- **构建工具**: Maven（`pom.xml`）
- **框架**: Spring Boot 3.3.0
- **入口类**: `GameMain`（游戏服）、`SceneMain`（场景服）、`SingleStartMain`（合并启动），详见 MEMORY
- **模块类型**: SLG 策略游戏服务端（多进程架构：game + scene，可合并启动）

## 编码规范

- 遵循标准 Java 代码规范，类、方法命名使用有意义的英文名，避免使用拼音
- 尽量使用 Lombok 减少样板代码（`@Data`、`@Getter`/`@Setter`、`@Builder` 等）
- 工厂方法统一命名为 `valueOf`，不使用 `of`、`create`
- 不要无意义地吞掉异常，至少记录日志；尽量使用自定义异常表达业务错误

## 类注释规范

- 每个类文件必须包含 JavaDoc 格式类注释
- 包含：类的简要描述（中文）、`@author`、`@date`（yyyy-MM-dd）
- 示例：
  ```java
  /**
   * 用户服务实现类
   *
   * @author yangxunan
   * @date 2025/12/18
   */
  ```

## AI 使用规则

- 项目记忆在 `.cursor/MEMORY.md`，涉及架构、命名、配置、协议等决策时先查阅
- 默认使用**简体中文**交流；类名/方法名/变量名/文件名用英文；代码注释用简体中文
- 修改前尽量阅读相关类/上下文，避免局部修改导致逻辑不一致
- 优先保持向后兼容，避免破坏现有对外接口
- 新建类放在合适的包结构下，避免在根包下堆积
- 删除文件前先标记 `@Deprecated`，再视情况彻底删除

## 文档规则

- 允许在 `.cursor/docs/` 下创建或更新与项目架构、扩展流程、AI 工作流相关的文档
- 根目录 `README.md` 及其他位置的 `.md` 文件仅在用户明确要求时才可创建或修改
- 代码注释和 JavaDoc 不受此限制

## 安全与配置

- 不要在仓库中硬编码密码、密钥等敏感信息
- 环境配置统一通过配置文件管理（`application.yml` 等）

## 测试与构建

- 新增业务逻辑时，优先考虑编写 JUnit 单元测试（除非用户明确要求，否则不主动创建测试类）
- 测试类放在 `src/test/java`，包结构与生产代码一致
- 保证 `mvn clean package` 成功执行

## Skill 维护规范

新增、修改或删除 Skill 时，需同步维护以下文件：

1. **Skill 文件本体**：`.cursor/skills/{skill-name}/SKILL.md`
2. **AI 工作流文档**：`.cursor/docs/ai-workflow.md` — 更新 Skill 速查表（新增/修改/删除条目）和相关工作流

## 提交规范

- 使用简短有意义的提交信息：`feat:` / `fix:` / `refactor:` 等前缀
- 大改动拆分为多个小 PR

## 模块与层级

- 一级：`slg-common`
- 二级：`slg-net`、`slg-redis`、`slg-support`
- 三级：`slg-shared-modules`、`slg-game`、`slg-scene`、`slg-robot`、`slg-web`、`slg-log`
- 四级：`slg-singlestart`
- **禁止越级依赖**，详见 MEMORY

## 领域模型

- 战斗、属性、进度等共享系统能力集中在 **slg-shared-modules**，game/scene 通过依赖该模块使用
- 战报 VO 由 slg-shared-modules 的 model 提供转化方法
- 高频接口注意性能和内存分配，避免热点路径中使用反射或重度对象创建
