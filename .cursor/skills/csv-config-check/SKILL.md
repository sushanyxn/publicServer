---
name: csv-config-check
description: 传入配置类文件，检查对应 CSV 配置格式是否正确；若 CSV 不存在则创建测试用 CSV。若配置类存在 @TableRefCheck 表关联，则一并检查或创建关联表的 CSV，创建测试数据时保证外键引用有效。在用户要求检查表配置、校验 CSV、补全缺失 CSV 或生成测试配置时使用。
---

# 配置类与 CSV 检查 / 生成测试 CSV

## 目标

1. **输入**：配置类文件路径（如 `slg-game/.../table/HeroTable.java`）。
2. **若 CSV 已存在**：检查 CSV 格式是否符合项目约定，必要时检查关联表。
3. **若 CSV 不存在**：按配置类与关联关系生成测试用 CSV；存在 `@TableRefCheck` 时先处理被引用表，再生成当前表，保证外键值在关联表中存在。

## 前置约定（项目 slg-support + MEMORY）

- **配置类**：带 `@Table`、至少一个 `@TableId` 主键；表名 = `@Table.alias()` 非空时取 alias，否则取类简单名（如 `HeroTable` → 文件名 `HeroTable.csv`）。
- **CSV 目录**：默认 `table/`（项目根下），由 `TableProperties.path` 配置，可在 `application.yml` 的 `table.path` 覆盖。
- **CSV 格式（Luban 风格）**：
  - 第 1 行：表头，列名与配置类**非 transient** 字段名一致。
  - 第 2 行：类型行（如 `int`,`string`,`array<IPlayerConsume>`），可与 `TableTypeConverter` 支持类型对应。
  - 第 3 行：注释行（中文说明等）。
  - 第 4 行起：数据行；列数需与表头一致，空行跳过。
- **表关联**：字段上的 `@TableRefCheck(RefTableClass.class)` 表示该字段为 RefTable 的主键引用；运行时由 `TableRefCheckManager` 校验引用存在性。生成测试 CSV 时，该字段的取值必须出现在 RefTable 的 CSV 数据中（如先为 RefTable 生成测试 CSV，再在当前表测试数据中引用其 id）。

## 执行步骤

### 1. 解析配置类文件

- 读取用户传入的配置类 Java 文件。
- 确认类上有 `@Table`（若为注入字段如 `TableInt<X>`，则目标配置类为泛型参数 `X`）。
- 收集：
  - **表名**：`@Table.alias()` 非空则用 alias，否则类简单名。
  - **主键字段**：带 `@TableId` 的字段名与类型（int/long 等）。
  - **所有非 transient 字段**：用于表头与列顺序（与 CSV 第 1 行一致）。
  - **关联字段**：带 `@TableRefCheck(RefClass)` 的字段及其引用类 `RefClass`。

### 2. 定位 CSV 路径

- 配置表目录：优先从项目 `application.yml`（如 slg-game、slg-scene）读取 `table.path`，缺省为项目根下的 `table`。
- CSV 完整路径：`{table.path}/{表名}.csv`。

### 3. CSV 已存在时的格式检查

- 文件存在则读取 CSV（UTF-8）：
  - 行数 ≥ 4（表头 + 类型行 + 注释行 + 至少一行数据）。
  - 第 1 行解析为表头，列名与配置类非 transient 字段名**一致**（顺序可放宽，但列名需一一对应，多列/少列需提示）。
  - 数据行（第 4 行起）列数与表头一致；空行可跳过。
- **关联检查**（可选深度）：
  - 对每个 `@TableRefCheck(RefClass)` 的字段，确认 RefClass 对应 CSV 存在且已加载或可解析；若需严格校验，可提示“运行时由 TableRefCheckManager 校验引用是否存在于 RefTable”。
  - 若用户明确要求“一并检查关联配置”，则对 RefClass 递归执行本 skill（检查其 CSV 是否存在与格式是否正确），再检查当前表 CSV 中该字段的值是否在 RefTable 的 id 列中出现。

### 4. CSV 不存在时：生成测试用 CSV

- **依赖顺序**：若当前配置类存在 `@TableRefCheck(RefClass)`，先对 **RefClass** 递归执行“生成测试 CSV”（若 RefClass 的 CSV 不存在则先为 RefClass 生成），再为当前类生成。
- **为当前类生成 CSV**：
  - 第 1 行（表头）：配置类所有非 transient 字段名，顺序与类中声明一致，逗号分隔。
  - 第 2 行（类型）：按字段类型写出类型名（如 `int`,`long`,`string`,`array<X>`,`List<X>` 等，与现有 table 下 CSV 风格一致）。
  - 第 3 行（注释）：可为字段中文说明或与表头相同。
  - 第 4 行起：至少 1 条测试数据。规则：
    - 主键：保证唯一（如 1001, 1002 或 1, 2）。
    - 带 `@TableRefCheck(RefClass)` 的字段：取值必须是 RefClass 的 CSV 中已存在的主键值（若刚为 RefClass 生成测试 CSV，则使用其中写入的 id，如 1001）。
    - 其他字段：按类型给合理占位（数字给 0 或 1，字符串给 `"测试"`，复杂类型可给简单合法示例或占位，如 `CurrencyReward|{GOLD:100}` 等，参考现有 MainTaskTable/HeroLevelTable）。
- 写入路径：`{table.path}/{表名}.csv`，编码 UTF-8。

### 5. 输出与收尾

- 若为检查：汇报“格式正确”或列出问题（缺列、列名不一致、行数不足等）及关联表检查结果。
- 若为生成：汇报已创建的 CSV 路径及已递归创建/检查的关联表列表。
- 不修改配置类源码，仅检查或生成 CSV。

## 简要检查清单

- [ ] 已解析配置类：表名、@TableId、非 transient 字段、@TableRefCheck 及 RefClass
- [ ] CSV 路径使用项目 table 目录 + 表名.csv
- [ ] 存在则校验：至少 4 行、表头与字段一致、数据列数一致；关联表按需递归检查
- [ ] 不存在则先递归处理所有 @TableRefCheck 引用表，再生成当前表测试 CSV，外键值来自关联表测试数据
- [ ] 生成 CSV 含表头、类型行、注释行、至少一条合法测试数据

## 参考

- 配置表加载：`slg-support` 的 `TableManager`、`TableLoadUtil`（Luban 格式至少 4 行）。
- 表名与元数据：`TableMeta`（表名 = alias 或类简单名）。
- 关联校验：`TableRefCheckManager`、`@TableRefCheck(Class)`。
- 现有示例：`table/HeroTable.csv`、`table/HeroLevelTable.csv`（HeroLevelTable 引用 HeroTable）、`table/MainTaskTable.csv`。
