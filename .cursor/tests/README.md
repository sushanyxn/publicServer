# 框架测试目录

本目录存放**底层框架**的测试计划与测试结果，供 Cursor 与框架测试 Skill 使用。

## 目录结构

```
.cursor/tests/
├── README.md           # 本说明
├── plans/              # 测试计划（按框架或总览）
│   └── *.md
└── results/            # 测试结果（按框架、日期或流水）
│   └── *.md 或 *.json
```

## 约定

- **plans**：每个框架可有一个测试计划文档，描述测试范围、用例要点、所需环境（单元/集成）。若无对应计划，框架测试 Skill 会先创建计划再开发测试代码。
- **results**：每次执行测试后在此记录结果（通过/失败、用例数、失败原因、时间等），便于对比与回归。

## 已纳入的框架

- 持久化框架（slg-support）
- 线程模型（slg-common）
- Redis 缓存（slg-redis）
- Redis Route（slg-net）

详见 `plans/` 下各计划文档。

## 集成测试模块

**slg-framework-test**：独立模块，使用 Testcontainers（MySQL/Redis）做端到端、性能与热点压力测试。

- 运行：`mvn test -pl slg-framework-test`（需 Docker）
- 说明：见 `slg-framework-test/README.md`
- 结果：可记录到 `results/`，命名建议 `framework-integration-result-{yyyyMMdd}.md`
