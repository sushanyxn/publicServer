---
name: git-commit
description: 根据用户描述的需要提交的内容，确定要纳入提交的文件，执行 git add 与 git commit，并生成符合项目规范的详细提交说明。在用户要求提交代码、做 git 提交、根据某次修改/功能/范围提交并写 commit message 时使用。
---

# Git 提交 Skill

根据用户输入的「需要提交的相关内容」描述，选定对应文件、暂存并提交，并自动生成**详细且符合项目规范**的提交说明。

## 触发场景

- 用户明确要求「提交 xxx」「把 xxx 提交 git」「为 xxx 写 commit」
- 用户描述提交范围或内容（如「Thrift 适配层」「.cursor 和 .gitignore」「登录相关修改」），需要据此选定文件并提交

## 提交信息规范（遵循 .cursorrules）

- **格式**：`<type>: <short description>`，首行简短且有意义
- **常用 type**：`feat`（新功能）、`fix`（修复）、`refactor`（重构）、`docs`（文档）、`chore`（构建/工具/配置等）
- **避免**：`update`、`fix bug` 等模糊表述
- **可选**：在首行后增加空行，再写多行详细说明（影响范围、原因、关键改动点）
- **语言**：提交说明统一使用**简体中文**书写（首行与正文均用中文）。

示例：

```
chore: 将 .cursor 纳入版本管理并更新 .gitignore

- 新增 .cursor 目录：MEMORY.md、plans、协议说明、各类 skills
- 修改 .gitignore：注释掉 .cursor/，使项目 .cursor 被版本跟踪
```

## 执行步骤

### 1. 确定待提交文件

- 若用户**明确给出路径或文件列表**：直接使用，并核对是否在仓库内、是否存在。
- 若用户**只给描述**（如「Thrift 相关」「.cursor 目录」「修复登录的改动」）：
  - 先执行 `git status`（或 `git status -s`）查看工作区与暂存区状态。
  - 根据描述语义匹配：
    - 模块/功能名 → 对应模块路径（如 slg-net、slg-game、.cursor 等）
    - 文件类型 → 如 `.gitignore`、`.cursor/**`、`**/thrift/**`
  - 从 `git status` 的 modified/untracked 列表中筛选出与描述**一致**的文件，避免无关文件（如临时文件、未完成的其他改动）被提交。
- 若描述模糊或存在歧义：向用户确认「是否只提交以下文件：…」再继续。

### 2. 生成提交说明

- **首行**：`<type>: <short description>`，英文、简短、说清「做了什么」。
- **正文**（可选）：若改动涉及多文件、多模块或需要说明原因/影响，在首行后空一行，写 2～5 条简要 bullet，每条一行：
  - 关键新增/修改点
  - 涉及模块或配置
  - 不写实现细节，只写「做了什么」和「影响范围」。
- 提交说明统一使用**简体中文**书写。

### 3. 执行提交

- 对确定的文件执行：`git add <path1> [path2 ...]`（或对目录 `git add <dir>/`）。
- 提交：`git commit -m "<首行>" -m "<正文>"`（若有正文）；仅首行则 `git commit -m "<首行>"`。
- 若 `git add` 或 `git commit` 失败（如冲突、无权限、无变更）：将错误信息反馈用户并说明原因，不强制完成提交。

### 4. 提交后反馈

- 简短说明本次提交了哪些文件（路径或模块）。
- 给出生成的完整 commit message，便于用户复制或修改。

## 注意事项

- **不提交**：未在用户描述范围内的文件、`target/`、IDE 配置、敏感信息等；若 `.gitignore` 已正确配置，通常不会误加。
- **不修改**：仅做 add + commit，不执行 `git push`、不修改分支、不执行 `rebase`/`merge`，除非用户明确要求。
- **单次提交范围**：一次 skill 调用只生成**一次**提交；若用户希望分多次提交，请分多次描述或明确「第一次提交 xxx，第二次提交 yyy」。

### 中文提交说明与编码（重要）

- 提交说明使用**简体中文**时，必须保证 Git 收到的是 **UTF-8** 编码，否则在 Windows 下易出现乱码（如 PowerShell 默认编码导致 `git commit -m "中文"` 写入错误编码）。
- **推荐做法**：将 commit message 写入临时文件（如 `commit_msg.txt`），确保文件以 **UTF-8 无 BOM** 保存，然后执行 `git commit -m "$(Get-Content commit_msg.txt -Encoding UTF8)"` 或 `git commit --amend -F commit_msg.txt`；或在 Bash/WSL 下执行 `git commit -m "中文内容"`。
- **避免**：在 Windows PowerShell 中直接使用 `git commit -m "含中文的长句"`，除非已设置 `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8` 且 Git 配置为 `core.quotepath=false`；仍建议用 **-F 文件** 方式传入 message，文件保存为 UTF-8。
- 提交前可用 `git log -1 --format=%B` 检查最近一条 message 是否显示正常。
