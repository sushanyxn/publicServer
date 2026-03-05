---
name: framework-test
description: 对项目底层框架执行测试。根据用户指定的框架读取或创建测试计划、查阅历史测试结果、决定是否修改或追加用例，执行测试并更新结果到 .cursor/tests/results。在用户要求对某一框架进行测试、跑框架测试、或回归测试时使用。
---

# 框架测试 Skill

当用户要求对**某一底层框架**进行测试时，按本 Skill 执行：先确保有测试计划与（可选）历史结果，再决定是否修改/追加测试内容，执行测试并更新结果。

## 框架与模块、计划对应关系

| 用户可能说的名称 | 模块 | 计划文件（优先） | 结果文件建议前缀 |
|------------------|------|------------------|------------------|
| 持久化、持久化框架、entity、EntityCache、AsyncPersistence | slg-support | persistence-测试计划.md | persistence-result- |
| 线程模型、执行器、Executor、KeyedVirtualExecutor、虚拟线程 | slg-common | executor-测试计划.md | executor-result- |
| Redis 缓存、redis 缓存、CacheAccessor、RedisCacheService | slg-redis | redis-cache-测试计划.md | redis-cache-result- |
| Redis Route、redis 路由、RpcRoute、RedisRoute、跨服 RPC | slg-net | redis-route-测试计划.md | redis-route-result- |

若 `plans/` 下没有对应的 `{框架}-测试计划.md`，则使用 **`.cursor/tests/plans/底层框架测试计划.md`** 中对应章节作为计划；若总览中也没有该框架描述，则**先创建**该框架的测试计划（可单独新建 `plans/{框架}-测试计划.md` 或补充总览），再继续后续步骤。

## 执行步骤

### 1. 确定要测的框架与模块

- 根据用户表述确定是哪一个或哪几个框架（持久化 / 线程模型 / Redis 缓存 / Redis Route）。
- 确定对应 Maven 模块：`slg-support` / `slg-common` / `slg-redis` / `slg-net`。

### 2. 读取测试计划（没有则先创建）

- 优先读取 `.cursor/tests/plans/{框架}-测试计划.md`（按上表命名）。
- 若不存在，读取 `.cursor/tests/plans/底层框架测试计划.md`，找到该框架对应章节。
- 若总览中也没有该框架的测试范围描述，则**先创建**测试计划：
  - 在 `plans/` 下新建 `{框架}-测试计划.md`，或
  - 在 `底层框架测试计划.md` 中新增一节，
  - 内容包含：目标模块与核心类、单元测试要点、集成测试要点（若需要）、建议用例或检查项。
- 以该计划作为本次测试的“范围与用例依据”。

### 3. 读取历史测试结果（有则参考）

- 在 `.cursor/tests/results/` 下查找该框架最近的结果文件（按上表建议前缀或文件名中的框架名、日期识别）。
- 若存在：阅读上次的通过/失败数、失败用例及原因。
- 用于下一步判断：是只跑现有用例、还是需要**修改**（如修复失败用例）或**追加**（如按计划补新用例）测试内容。

### 4. 决定是否修改或追加测试内容

- **仅执行**：若计划与现有代码一致、且用户仅要求“跑一遍”，则直接执行现有测试。
- **先修改再执行**：若历史结果中有失败，或计划中有用例尚未实现，则先：
  - 修复失败用例（代码或断言），或
  - 按计划在对应模块 `src/test/java` 下新增/补全测试类与方法，
  - 再执行测试。
- **追加用例**：若计划中有明确列出但尚未覆盖的要点，可新增测试方法并一并执行。
- 向用户简短说明本次是“只跑”“先修再跑”还是“追加用例后跑”。

### 5. 执行测试

- 在项目根目录执行：`mvn test -pl <模块>`，例如：
  - 持久化：`mvn test -pl slg-support`
  - 线程模型：`mvn test -pl slg-common`
  - Redis 缓存：`mvn test -pl slg-redis`
  - Redis Route：`mvn test -pl slg-net`
- 若需一次跑多个框架：对多个模块分别执行，或使用 `mvn test -pl slg-common,slg-support,slg-redis,slg-net`（按需）。
- 若有集成测试模块（如 `slg-framework-test`），按需执行：`mvn test -pl slg-framework-test`。
- 收集输出：通过/失败/跳过数量、失败用例名称与错误信息。

### 6. 更新测试结果

- 在 `.cursor/tests/results/` 下**新增或覆盖**该框架的结果文件。
- 命名建议：`{框架}-result-{yyyyMMdd}.md` 或带时间 `{框架}-result-{yyyyMMdd-HHmm}.md`；若希望保留历史可每次新文件不覆盖。
- 内容建议包含：
  - 执行时间（日期或时间点）；
  - 涉及模块与测试类型（单元/集成）；
  - 用例总数、通过数、失败数、跳过数；
  - 失败用例名称及简要原因（或贴关键栈信息）；
  - 与上次结果对比（若读过历史）：新增/修复/仍失败。
- 写完后可简短告知用户“测试结果已更新到 `.cursor/tests/results/...`”。

## 注意事项

- 测试代码位置：单元测试放在**各框架所在模块**的 `src/test/java`，包名与源码对应；不要只在“测试程序”里写一次性脚本而不落库。
- 依赖：各模块测试依赖由根 pom 的 dependencyManagement 统一管理（junit-jupiter、mockito）；若某模块未继承到 test 依赖，需在该模块 pom 中显式声明 test 作用域依赖。
- 集成测试：若需要真实 Redis/MySQL，使用 Testcontainers 或 `@ActiveProfile("test")`；集成用例可放在独立模块 `slg-framework-test` 或各模块的 test 目录下。
- 用户若未指定框架：可列出四类框架让用户选择，或按“最近改动/风险高”的模块建议先测某一项。

## 触发场景

- 用户说“对持久化框架做测试”“测一下线程模型”“跑 Redis 缓存的测试”“Redis Route 回归测试”等；
- 用户说“对某一框架进行测试”“框架测试”“底层框架测试”并指定或可推断出框架名；
- 用户要求“根据测试计划跑测试并更新结果”。
