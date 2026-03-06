---
name: git-commit
description: 根据用户描述的需要提交的内容，确定要纳入提交的文件，按「变更类型」划分并分次提交，提交说明中高亮标注涉及范围（框架层/业务层/cursor工具）。在用户要求提交代码、做 git 提交、根据某次修改/功能/范围提交并写 commit message 时使用。
---

# Git 提交 Skill

根据用户输入的「需要提交的相关内容」描述，选定对应文件、**按变更类型划分后分次提交**，每次提交的说明中**高亮标注**涉及范围（框架层 / 业务层 / cursor 工具），并生成符合项目规范的提交说明。

## 变更类型划分（必须遵守）

提交前必须将改动归类为以下**三种之一**，且**不同类型必须分开提交**，不得在一次提交中混合多种类型。

| 类型 | 含义 | 典型路径/范围 |
|------|------|----------------|
| **框架层** | 通用框架、网络、存储、配置表等与具体业务无关的基础能力 | `slg-common/`、`slg-net/`、`slg-redis/`、`slg-support/`、`slg-table/`、根目录 `pom.xml`、各模块 `pom.xml` 中的依赖与插件 |
| **业务层** | 游戏玩法、场景、战斗、具体功能逻辑 | `slg-game/`、`slg-scene/`、`slg-fight/`、`slg-web/`、`slg-log/`、`slg-robot/`、`slg-singlestart/` 等业务模块及其配置 |
| **cursor 工具** | Cursor 与项目协作相关的配置、技能、计划、规则 | `.cursor/` 目录（如 `plans/`、`skills/`、`rules/`、`protocol/`、`MEMORY.md` 等）、`.cursorrules`、`.gitignore` 中与 .cursor 相关部分 |

- 若一批改动中**同时包含多种类型**：按类型拆成多批，**每批单独执行一次** `git add` + `git commit`，并在提交说明中明确标注该次提交的【框架层】/【业务层】/【cursor 工具】。
- 若用户**仅描述一种范围**（如「只提交 .cursor 的修改」）：只提交该类型，并在说明中标注对应类型。
- 若单文件可能跨层（如根 `pom.xml` 仅改版本号算框架层）：按主要影响归类，有歧义时向用户确认。

## 触发场景

- 用户明确要求「提交 xxx」「把 xxx 提交 git」「为 xxx 写 commit」
- 用户描述提交范围或内容（如「协议解析层」「.cursor 和 .gitignore」「登录相关修改」），需要据此选定文件、**按类型分次**提交

## 提交信息规范（遵循 .cursorrules）

- **格式**：首行 `<type>: <short description>`，且必须在正文中**高亮标注**本次提交涉及的变更类型（三选一）。
- **变更类型标注**（必填）：在正文首条或单独一行写明，格式为以下之一：
  - `【框架层】` 或 `涉及范围：框架层`
  - `【业务层】` 或 `涉及范围：业务层`
  - `【cursor 工具】` 或 `涉及范围：cursor 工具`
- **常用 type**：`feat`（新功能）、`fix`（修复）、`refactor`（重构）、`docs`（文档）、`chore`（构建/工具/配置等）
- **避免**：`update`、`fix bug` 等模糊表述
- **可选**：在首行后增加空行，再写多行详细说明（**先写变更类型标注**，再写影响范围、原因、关键改动点）
- **语言**：提交说明统一使用**简体中文**书写（首行与正文均用中文）。

示例（框架层）：

```
refactor: 移除 Thrift 协议及编码转换层

【框架层】
- 删除 slg-net 下 com.slg.net.thrift 包及 generated 生成代码
- 根 / slg-net / slg-game 的 pom.xml 移除 libthrift 依赖
```

示例（cursor 工具）：

```
chore: 将 .cursor 纳入版本管理并更新 .gitignore

【cursor 工具】
- 新增 .cursor 目录：MEMORY.md、plans、协议说明、各类 skills
- 修改 .gitignore：注释掉 .cursor/，使项目 .cursor 被版本跟踪
```

## 执行步骤

### 1. 确定待提交文件并按类型划分

- 若用户**明确给出路径或文件列表**：直接使用，并核对是否在仓库内、是否存在；再按「变更类型划分」表将文件归为框架层 / 业务层 / cursor 工具。
- 若用户**只给描述**：
  - 先执行 `git status`（或 `git status -s`）查看工作区与暂存区状态。
  - 根据描述语义匹配文件（模块路径、文件类型等），再从列表中按「变更类型划分」表归类。
- **若本次涉及多种类型**：将文件分成多组，**每组对应一次提交**；先向用户说明「将按类型分 N 次提交」，再依次执行（见步骤 3）。
- 若描述模糊或存在歧义：向用户确认「是否只提交以下文件：…」及「本次仅提交 [框架层/业务层/cursor 工具] 中的哪一类」再继续。

### 2. 生成提交说明（含变更类型高亮）

- **首行**：`<type>: <short description>`，简短、说清「做了什么」。
- **正文**：**必须**包含一行变更类型标注：`【框架层】` / `【业务层】` / `【cursor 工具】`（或「涉及范围：xxx」）。其余 bullet 写关键改动、涉及模块或配置，不写实现细节。
- 提交说明统一使用**简体中文**书写。

### 3. 执行提交（分类型、分次）

- **一次只提交同一类型的文件**。若有多类：先提交一类（如先框架层），完成后再提交下一类（如业务层），依此类推。
- 对**当前类型**的文件执行：`git add <path1> [path2 ...]`（或 `git add <dir>/`）。
- 提交：使用 **-F 文件** 方式传入 commit message。**推荐**用编辑器新建 `.commit_msg` 或 `.commit_msg.txt`，写好内容并保存为 **UTF-8**（Cursor/VS Code 默认即为 UTF-8），然后 `git commit -F <该文件>`；提交后用 `git log -1 --format=%B` 验证中文无误后删除该文件。若由命令行/脚本动态生成文件内容（如 PowerShell 下），见下方「PowerShell 与提交信息编码」。
- 若存在多种类型：重复「为该类型 add → 写 message（含对应【框架层】/【业务层】/【cursor 工具】）→ commit」，直到所有类型都已单独提交完毕。
- 若 `git add` 或 `git commit` 失败：将错误信息反馈用户并说明原因，不强制完成提交。

### 4. 提交后反馈

- 说明本次共完成几次提交、每次的变更类型（框架层/业务层/cursor 工具）。
- 列出每次提交涉及的文件（路径或模块）及完整 commit message，便于用户复制或修改。

## 注意事项

- **不提交**：未在用户描述范围内的文件、`target/`、IDE 配置、敏感信息等；若 `.gitignore` 已正确配置，通常不会误加。
- **不修改**：仅做 add + commit，不执行 `git push`、不修改分支、不执行 `rebase`/`merge`，除非用户明确要求。
- **分类型提交（强制）**：**框架层、业务层、cursor 工具**三类改动必须分次提交，不得在一次提交中混合多种类型；一次 skill 调用可产生多笔提交，每笔对应一种类型。

### 中文提交说明与编码（重要）

- 提交说明使用**简体中文**时，必须保证 Git 收到的是 **UTF-8** 编码，否则在 Windows 下易出现乱码。
- **推荐做法**：用**编辑器**新建临时文件（如 `.commit_msg` 或 `.commit_msg.txt`），写好 commit message，保存为 **UTF-8**（Cursor/VS Code 默认即为 UTF-8 无 BOM），然后 `git commit -F <该文件>`；提交完成后删除该文件。用 .txt 无妨，提交后记得删或把该文件名加入 .gitignore 即可。
- 提交后必须用 `git log -1 --format=%B` 检查最近一条 message 是否显示正常；若出现乱码，用编辑器重新保存为 UTF-8 后 `git commit --amend -F <该文件>` 修正。

### PowerShell 与提交信息编码（仅当用命令行/脚本生成文件时）

**最省事的方式**：用编辑器直接写 `.commit_msg` 或 `.commit_msg.txt`，保存为 UTF-8，再 `git commit -F <文件>`，不经过 PowerShell/终端转码，一般不会乱码。

仅当**必须用命令行或脚本动态生成** commit message 文件内容时（例如在自动化或终端里拼字符串写文件），在 **Windows + PowerShell** 下需注意：终端默认编码往往不是 UTF-8，用 PowerShell 的 `"中文"`、here-string、或 `python -c "msg='中文'"`（这里的「中文」经终端传入）写文件，容易按 GBK 等写入，Git 按 UTF-8 读就会乱码。

- **禁止**：在 PowerShell 里用 `"中文"`、here-string 等写含中文的内容到文件；`git commit -m "中文"`；在不确定终端编码时用 `python -c "msg='中文'"` 且中文直接出现在命令里。
- **若必须用脚本生成**（任选其一）：
  1. **用编辑器写 .txt**：在项目里新建 `.commit_msg.txt`，编辑器里写好内容并保存为 UTF-8，再 `git commit -F .commit_msg.txt`（推荐，最省事）。
  2. **Python 写文件，中文用 Unicode 转义**：脚本内仅 ASCII，避免终端编码影响，例如  
     `python -c "msg='fix: \u4f18\u5316 ...\n\n'; open('.commit_msg','wb').write(msg.encode('utf-8'))"`
  3. **Python 写文件，显式 UTF-8**：`open('.commit_msg','w',encoding='utf-8',newline='\n').write(msg)`，且确保该进程未受终端编码影响。
