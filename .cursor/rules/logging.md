---
description: 日志框架、日志级别规范与模块特例
alwaysApply: true
---

# 日志规范

## 日志工具

- 使用 `LoggerUtil` 工具类（`com.slg.common.log.LoggerUtil`，内部封装 SLF4J）
- 业务层**禁止**直接使用 SLF4J Logger
- **禁止**在生产代码中使用 `System.out.println` 做日志

## 日志级别

| 级别 | 用途 |
|------|------|
| `debug` | 调试信息、详细流程跟踪 |
| `info` | 正常流程的关键节点（如启动完成、玩家登录等） |
| `warn` | 警告信息、可恢复的异常、参数验证失败 |
| `error` | 错误信息、不可恢复的异常、系统故障 |

## 模块特例

- **`slg-support` 模块**：所有正常日志使用 `debug`，错误日志使用 `error`

## 使用原则

- 避免重复输出多行 debug 日志
- 正常流程选取关键信息进行 info 级别输出
