---
description: 进度管理系统规范
globs:
  - "**/progress/**"
---

# 进度管理系统规范

## 进度类型

- 枚举 `ProgressOwnerEnum` 及进度类型接口定义在 `slg-shared-modules` 的 `com.slg.sharedmodules.progress.type` 包
- 每个枚举值持有独立的进度映射表，不同类型进度数据隔离

## 进度条件

- 实现 `IProgressCondition<T, E>` 接口（T = 拥有者类型，E = 事件类型）
- 条件类放在业务模块的 `progress.condition` 包
- 系统自动扫描并建立条件到事件的映射关系

## 进度事件

- 实现 `IProgressEvent<T>` 接口
- 必须实现 `getOwnerEnum()` 和 `getOwnerId()`
- 事件类放在业务模块的 `progress.event` 包

## 序列化注意事项

- `ProgressMeta` 支持 JSON 序列化，`type` 字段序列化为 `typeId`
- 反序列化后需通过 `IProgressTypeTransform` 重新设置 `type` 对象
- 回调函数 `whenUpdate` 和 `whenFinish` 不参与序列化，需在反序列化后重新设置
