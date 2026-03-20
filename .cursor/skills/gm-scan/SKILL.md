---
name: gm-scan
description: 扫描游戏内所有 GM 指令，生成 GM 文档。扫描 slg-game 下所有实现 IGMCommand 接口的类，提取其中的 GM 方法、JavaDoc 注释和参数信息，按模块生成文档到 .cursor/gm/ 目录。在用户要求扫描 GM 指令、生成 GM 文档、查看 GM 指令列表或更新 GM 文档时使用。
---

# GM 指令扫描与文档生成

## 目标

- 扫描 `slg-game` 下所有实现 `IGMCommand` 接口的类。
- 提取每个 GM 方法的名称、参数、JavaDoc 注释。
- 按模块生成 GM 指令文档，输出到 `.cursor/gm/` 目录。

## 输入

- 无必传参数；可选指定模块名以缩小范围，未指定则处理全部 GM 指令类。

## GM 指令来源

- **GM 指令类**：`slg-game` 下所有实现 `IGMCommand` 接口的类（通常在 `gm/command/` 包下，但也可能在各业务模块中）。
- **方法识别规则**：非静态、非合成的实例方法，且第一个参数为 `Player`。
- **注册键**：`方法名小写_用户参数数量`（用户参数 = 总参数 - Player 参数）。
- **参数类型**：支持 `String`、`int/Integer`、`long/Long`、`double/Double`、`float/Float`、`boolean/Boolean`、`short/Short`。

## 输出规范

- **目录**：`.cursor/gm/`（若不存在则创建）。
- **主文件**：`.cursor/gm/GM指令总览.md` — 全部 GM 指令的索引与速查表。
- **模块文件**：`.cursor/gm/{模块名}.md` — 按 GM 指令类名推断模块（如 `HeroGMCommand` → `hero.md`）。

### 主文件格式

```markdown
# GM 指令总览

> 自动生成，请勿手动编辑。如需更新请重新执行 gm-scan skill。
> 最后更新：{日期}

## 速查表

| 指令名 | 参数 | 所属模块 | 说明 |
|--------|------|----------|------|
| gainHero | heroId(int) | 英雄 | 获得指定英雄 |
| gainAllHero | 无 | 英雄 | 获得全部英雄 |
| ... | ... | ... | ... |

## 模块索引

- [英雄](hero.md)
- ...
```

### 模块文件格式

```markdown
# {模块中文名} GM 指令

> 来源类：`{完整类名}`

## 指令列表

### gainHero

- **用法**：`gainHero <heroId>`
- **参数**：
  - `heroId` (int) — 英雄ID
- **说明**：获得指定英雄
- **返回**：0=成功, 1=失败

### gainAllHero

- **用法**：`gainAllHero`
- **参数**：无
- **说明**：获得全部英雄
- **返回**：0=成功, 1=失败
```

## 执行步骤

### 1. 扫描 GM 指令类

- 在 `slg-game` 下查找所有实现 `IGMCommand` 接口的 Java 类。
- 搜索策略：
  - 先搜索 `implements IGMCommand` 关键字。
  - 也检查 `gm/command/` 包下的所有类。

### 2. 解析每个 GM 方法

对每个 GM 指令类中的方法：

- **识别条件**：非静态、非合成、第一个参数为 `Player`。
- **提取信息**：
  - 方法名（即 GM 指令名，调用时不区分大小写）
  - 用户参数列表（排除第一个 Player 参数）：参数名、类型
  - JavaDoc 注释：首段作为"说明"，`用法:` 行作为用法示例
  - 返回值含义：`0=成功(GMService.SUCCESS), 非0=失败(GMService.FAIL)`

### 3. 推断模块名

- 从类名推断：去掉 `GMCommand` 后缀，如 `HeroGMCommand` → `hero`（英雄）。
- 中文模块名：根据类注释或类名推断（如 Hero→英雄、City→城市、Alliance→联盟）。
- 若无法推断，使用类名本身作为模块名。

### 4. 生成文档

- 创建 `.cursor/gm/` 目录（若不存在）。
- 按模块分别生成 `{模块名}.md` 文件。
- 生成 `GM指令总览.md` 索引文件，包含速查表和模块索引。

### 5. 收尾

- 确认 `.cursor/gm/` 下文件已生成。
- 回复中给出文档路径与 GM 指令统计。

## 注意事项

- **只读操作**：不修改任何 Java 源代码，仅生成文档。
- **JavaDoc 优先**：方法说明优先使用 JavaDoc 注释；无注释时根据方法名推断。
- **参数名**：Java 编译后参数名可能丢失，优先从 JavaDoc 的 `@param` 提取；无则使用类型名。
- **增量更新**：若 `.cursor/gm/` 已有文件，覆盖更新（文档为自动生成，每次全量重写）。

## 检查清单

- [ ] 已扫描 slg-game 下所有 IGMCommand 实现类
- [ ] 每个 GM 方法均提取了名称、参数、说明
- [ ] 文档已写入 .cursor/gm/ 目录，格式清晰可读
- [ ] GM指令总览.md 包含完整速查表
- [ ] 未修改任何 Java 源代码
