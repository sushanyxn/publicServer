---
name: message-table-sync
description: 扫描 MessageTable.csv 消息表与 InfoId 常量接口，检查两者一致性：CSV 中有而 InfoId 缺少的常量自动补全到接口；InfoId 中有而 CSV 缺少的记录自动补全到 CSV（内容使用注释文字）；注释与 CSV 内容不一致时以 CSV 为准修正注释。在用户要求同步消息表、检查 InfoId 与 CSV 一致性、补全消息常量或消息配置时使用。
---

# 消息表（MessageTable）与 InfoId 常量同步

## 目标

1. 确保 `table/MessageTable.csv` 中每条消息在 `InfoId` 常量接口中都有对应定义。
2. 确保 `InfoId` 中每个常量在 `MessageTable.csv` 中都有对应记录。
3. 双方内容（InfoId 注释 vs CSV content 列）保持一致，不一致时以 CSV 为准修正注释。

## 前置约定

- **MessageTable.csv 路径**：`table/MessageTable.csv`，Luban 风格（第 1 行表头，第 2 行类型，第 3 行注释，第 4 行起数据）。
- **InfoId 接口路径**：`slg-common/src/main/java/com/slg/common/constant/InfoId.java`，使用 `public static final int` 常量，每个常量上方有 `/** 中文说明 */` 注释。
- **常量命名规范**：全大写下划线分隔，名称应能反映消息含义（如 `HERO_NOT_FOUND`）。
- **CSV content 列**：消息的中文显示内容，客户端直接展示。
- **CSV id 列**：整数，与 InfoId 常量的值一一对应。

## 执行步骤

### 1. 读取 MessageTable.csv

- 读取 `table/MessageTable.csv`。
- 解析第 1 行为表头（`id`, `content`），第 4 行起为数据行。
- 收集所有 `{id → content}` 映射。

### 2. 读取 InfoId 接口

- 读取 `slg-common/src/main/java/com/slg/common/constant/InfoId.java`。
- 解析所有 `int 常量名 = 值;` 及其上方的 JavaDoc 注释文本。
- 收集所有 `{值 → (常量名, 注释)}` 映射。

### 3. 差异比对

对比两份映射，分为以下几类：

| 情况 | CSV 有 | InfoId 有 | 处理 |
|------|--------|-----------|------|
| CSV 多出 | ✓ | ✗ | 在 InfoId 中补充常量（名称从 content 推导，值为 id） |
| InfoId 多出 | ✗ | ✓ | 在 CSV 中追加数据行（id 为常量值，content 为注释文本） |
| 注释不一致 | ✓ | ✓（注释 ≠ content） | 以 CSV content 为准，修正 InfoId 注释 |
| 一致 | ✓ | ✓（注释 = content） | 无需处理 |

### 4. 补全 InfoId 常量

对 CSV 中有而 InfoId 中缺少的记录：

- **常量名推导**：取 CSV 的 `content` 文本，翻译或转换为全大写下划线分隔的英文标识符。例如：
  - "英雄不存在" → `HERO_NOT_FOUND`
  - "资源不足" → `RESOURCE_NOT_ENOUGH`
  - 若无法自动翻译，使用 `INFO_` + id 作为兜底命名，并在注释中标注 content 原文。
- **追加位置**：在 InfoId 接口末尾（`}` 之前）按 id 升序追加。
- **格式**：
  ```java
  /** {content} */
  int CONSTANT_NAME = {id};
  ```

### 5. 补全 MessageTable.csv

对 InfoId 中有而 CSV 中缺少的常量：

- 在 CSV 末尾追加数据行：`{常量值},{注释文本}`。
- 若注释为空则 content 列写 `未定义消息`。

### 6. 修正注释不一致

对两边都存在但 InfoId 注释与 CSV content 不同的条目：

- 修改 InfoId 中该常量上方的 `/** ... */` 注释为 CSV 中的 content。

### 7. 收尾输出

- 汇总报告：
  - 已补充到 InfoId 的常量列表（名称、值、来源 CSV content）
  - 已追加到 CSV 的记录列表（id、content、来源 InfoId 常量名）
  - 已修正注释的常量列表（旧注释 → 新注释）
  - 无变更的记录数量
- 若 CSV 或 InfoId 文件被修改，列出修改的文件路径。

## 简要检查清单

- [ ] 已读取并解析 `table/MessageTable.csv` 全部数据行
- [ ] 已读取并解析 `InfoId.java` 全部常量定义与注释
- [ ] CSV 多出的 id 已补充为 InfoId 常量（名称合理、注释使用 content）
- [ ] InfoId 多出的常量已追加为 CSV 数据行（content 使用注释）
- [ ] 注释不一致的已以 CSV 为准修正
- [ ] InfoId 中常量按 id 值有序排列
- [ ] CSV 数据行按 id 值有序排列
- [ ] 输出变更汇总报告

## 参考

- InfoId 接口示例：`slg-common/src/main/java/com/slg/common/constant/InfoId.java`
- LoginCode 接口（同类常量接口格式参考）：`slg-common/src/main/java/com/slg/common/constant/LoginCode.java`
- 表配置 CSV 格式：`table/` 目录下各 `*.csv`（Luban 风格 4 行起始）
- 客户端读取 MessageTable 的配置类：`slg-client/.../config/ClientMessageTable.java`
