---
description: Agent 热更框架与 Groovy 脚本引擎的使用规范
globs:
  - "**/hotreload/**"
  - "**/script/**"
  - "**/*.groovy"
---

# 热更框架与 Groovy 脚本规范

## 概述

项目提供两种运行时动态能力：
- **Agent 热更**：基于 `java.lang.instrument.Instrumentation`，运行时替换已有类字节码或加载全新类
- **Groovy 脚本**：通过 `GroovyShell` 在运行时执行任意脚本，可访问 Spring Bean

两者代码均位于 `slg-common` 模块：
- `com.slg.common.hotreload` -- 热更框架
- `com.slg.common.script` -- Groovy 脚本引擎

## 前置条件

### 启动参数

所有需要热更能力的进程，启动时必须添加 `-javaagent` 参数：

```bash
java -javaagent:lib/slg-common-1.0-SNAPSHOT.jar -jar slg-game.jar
```

若未添加此参数，`HotReloadManager.reload()` 会返回失败提示，不会抛异常。

### 打包方式

项目采用 **thin JAR + lib/ 目录** 部署：
- `mvn package` 后在 `target/` 下生成 thin JAR + `target/lib/`（所有依赖）
- `slg-common.jar` 既是依赖（在 lib/ 中）又是 Agent JAR

## 热更接口

### HotReloadManager.reload(String dirPath)

- 传入一个目录路径，该目录视为 classpath 根目录
- 自动递归扫描所有 `.class` 文件
- 按文件路径推导全限定类名（如 `com/slg/game/Foo.class` → `com.slg.game.Foo`）
- 返回 `HotReloadResult`，包含每个类的处理详情

### 热更能力边界

**可以热更：**
- 修改已有类的方法体逻辑
- 修改 lambda 表达式体
- 加载全新类（defineClass）

**不可以热更（JVM 限制）：**
- 已有类新增/删除方法
- 已有类新增/删除字段
- 修改方法签名（参数/返回值）
- 修改类继承关系（extends/implements）
- 修改 enum 常量、record 组件

### 全新类注意事项

- 新类通过 `ClassLoader.defineClass()` 注入应用 ClassLoader
- 新类可以被热更后的已有类正常引用
- 新类**不会**自动注册为 Spring Bean
- 重复热更同一目录是安全的（幂等）：首次为新类的会走 defineClass，之后自动变为 redefineClasses

## Groovy 脚本接口

### GroovyScriptManager.execute(String script)

- 传入 Groovy 脚本字符串
- 返回 `ScriptExecuteResult`，包含返回值、println 输出、错误信息

### 预置变量

| 变量名 | 类型 | 说明 |
|--------|------|------|
| `ctx` / `applicationContext` | ApplicationContext | Spring 容器，可通过 `ctx.getBean(...)` 获取任意 Bean |
| `log` | Logger | SLF4J Logger |
| `out` | PrintWriter | println 输出捕获 |

### 安全约束

- **禁止对外暴露** Groovy 脚本接口，仅限内部 GM 工具或运维通道
- 脚本输出限制 64KB，超出自动截断
- 脚本执行使用 ReentrantLock 串行化

## 示例脚本

位于 `slg-common/src/main/resources/script-demo/`：
- `demo_query_bean.groovy` -- 查询 Spring Bean 信息
- `demo_hot_reload.groovy` -- 通过脚本触发热更
- `demo_system_info.groovy` -- 查询 JVM 系统信息

## 编码规范

- 热更相关新增类放在 `com.slg.common.hotreload` 包下
- 脚本相关新增类放在 `com.slg.common.script` 包下
- Groovy 脚本文件使用 `.groovy` 后缀
- 热更和脚本操作必须记录日志（INFO 级别记录操作、WARN 级别记录失败）
