# Agent 热更 & Groovy 脚本测试结果

**执行时间**: 2026-03-10 10:26  
**范围**: `slg-framework-test` 模块中 `HotReloadE2EIntegrationTest` + `GroovyScriptE2EIntegrationTest`  
**环境**: Windows 10, Maven 3.x, JDK 21.0.9, `-javaagent:slg-common-1.0-SNAPSHOT.jar`

## 汇总

| 指标     | 数值 |
|----------|------|
| 用例总数 | 16   |
| 通过     | 16   |
| 失败     | 0    |
| 错误     | 0    |
| 跳过     | 0    |

**BUILD SUCCESS**，总耗时 12.977s

## 各测试类详情

| 测试类                            | 类型       | 用例数 | 通过 | 耗时    |
|----------------------------------|------------|--------|------|---------|
| HotReloadE2EIntegrationTest      | 热更集成   | 7      | 7    | 4.217s  |
| GroovyScriptE2EIntegrationTest   | 脚本集成   | 9      | 9    | 3.192s  |

## HotReloadE2EIntegrationTest 用例详情

| # | 用例名称                                | 状态 | 说明 |
|---|----------------------------------------|------|------|
| 1 | Instrumentation 已通过 -javaagent 注入  | PASS | `InstrumentationHolder.isAvailable()` 为 true |
| 2 | 重定义已有类：方法体变更生效              | PASS | 动态编译修改版 HotReloadTestTarget，热更后返回值从原始变为 "hot-reloaded" |
| 3 | 加载全新类：defineClass 成功后可反射调用  | PASS | 全新类 `BrandNewClass` 加载成功，反射调用 `hello()` 返回预期值 |
| 4 | 混合热更：新类 + 已有类同时热更           | PASS | 2 个类（1 个新类 + 1 个已有类）全部成功，框架自动先 define 再 redefine |
| 5 | 重复热更同一目录具有幂等性                | PASS | 连续两次热更同一目录，两次均返回 `isAllSuccess=true` |
| 6 | 空目录返回错误信息                        | PASS | 返回 `hasError=true`，message 包含"未找到 .class 文件" |
| 7 | 不存在的目录返回错误信息                  | PASS | 返回 `hasError=true`，message 包含"目录不存在" |

### 热更性能参考

| 操作                     | 耗时   |
|--------------------------|--------|
| 单类 redefineClasses     | ~19ms  |
| 单类 defineClass（新类） | ~8ms   |
| 混合热更 2 类            | ~20ms  |
| 幂等重复热更             | ~15ms  |

## GroovyScriptE2EIntegrationTest 用例详情

| # | 用例名称                      | 状态 | 说明 |
|---|------------------------------|------|------|
| 1 | 简单表达式求值                | PASS | `"1 + 1"` 返回 Integer(2) |
| 2 | 访问预置绑定变量 ctx          | PASS | `ctx.getBeanDefinitionNames().length > 0` 返回 true |
| 3 | 通过 ctx.getBean 获取 Bean    | PASS | 获取 GroovyScriptManager Bean 成功 |
| 4 | println 输出被正确捕获        | PASS | `out.println` 内容出现在 `result.output` 中 |
| 5 | 传入额外绑定变量              | PASS | `${name}` 插值正确，返回 "Hello, SLG!" |
| 6 | 脚本语法错误返回 error result | PASS | 语法错误脚本返回 `success=false`，`errorMessage` 包含编译错误 |
| 7 | 脚本运行时异常被正确捕获      | PASS | `throw new RuntimeException("test error")` 被捕获，message 包含 "test error" |
| 8 | 空脚本返回错误                | PASS | 空字符串返回 `success=false`，message 包含"为空" |
| 9 | 脚本无返回值时正常执行        | PASS | `"def x = 42"` 执行成功，`success=true` |

### 脚本执行性能参考

| 操作                        | 耗时   |
|-----------------------------|--------|
| 首次执行（含 Groovy 初始化）| ~153ms |
| 后续简单表达式              | ~8ms   |
| 访问 Spring Bean            | ~76ms  |
| 字符串插值                  | ~21ms  |
| 异常捕获                    | ~14ms  |

## 首次执行修复的问题

1. **Groovy GString 类型差异**：`"Hello, ${name}!"` 返回 Groovy `GStringImpl` 而非 Java `String`，断言 `isEqualTo("Hello, SLG!")` 失败。修复：改用 `.toString()` 再比较。
2. **测试执行顺序依赖**：多个热更测试共享 `HotReloadTestTarget` 静态类，JUnit 5 默认不保证执行顺序。`idempotentReload` 先于 `redefineExistingClass` 执行导致初始值断言失败。修复：添加 `@TestMethodOrder(OrderAnnotation.class)` + `@Order` 固定顺序。

## 与上次框架测试结果对比

| 项目                    | 上次 (2026-03-05) | 本次 (2026-03-10) |
|------------------------|--------------------|--------------------|
| 总用例数               | 18                 | 18 + 16 = 34      |
| 新增测试类             | -                  | HotReloadE2EIntegrationTest, GroovyScriptE2EIntegrationTest |
| 新增框架能力           | -                  | Agent 热更（含全新类加载）、Groovy 脚本引擎 |

## 执行命令

```bash
mvn clean test -pl slg-framework-test -Dtest="HotReloadE2EIntegrationTest,GroovyScriptE2EIntegrationTest" -DskipTests=false
```
