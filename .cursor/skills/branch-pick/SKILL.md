---
name: branch-pick
description: 根据当前分支与用户需求，从其他分支合并或拣选（cherry-pick）指定内容到当前分支。知晓本仓库 master（主干/框架）与 thewartest（thewar 测试/业务）的角色划分，在用户要求从某分支同步框架、同步业务、同步 cursor 工具或按提交拣选代码时使用。
---

# 分支代码 Pick Skill

根据**当前所在分支**与用户输入的**需求**，从指定分支合并或拣选对应内容到当前分支。**强制要求**：用户提出 pick 要求后，必须先**整理 pick 内容**、**比对分支间代码差异**，并完成**二次确认**，方可执行实际操作（merge / cherry-pick / checkout 等）。

## 本仓库分支约定

| 分支 | 角色 | 内容范围 | 与另一分支的关系 |
|------|------|----------|------------------|
| **master** | 主干 | 主要框架 + 少量关键业务；技术方案沉淀；通用 cursor skills（.cursor/ 等） | 框架与能力基线，thewartest 在其基础上开发 |
| **thewartest** | thewar 测试分支 | 以 master 为基础；Thrift 协议转换器（在框架上做编解码）；大量业务开发以验证框架 | 框架需与 master 保持一致，业务独立演进 |

- **框架**：与具体业务无关的通用能力，如 slg-common、slg-net、slg-redis、slg-support、slg-table、根/模块 pom 等。
- **业务**：thewar 相关玩法、场景、战斗、协议与业务逻辑，多在 slg-game、slg-scene、slg-shared-modules 等。
- **cursor 工具**：.cursor/（plans、skills、rules、protocol、MEMORY.md 等）、.cursorrules、.gitignore 中与 .cursor 相关部分。

## 触发场景

- 用户要求「从 master 同步框架到 thewartest」「把 thewartest 的某某功能合到 master」
- 用户要求「从某分支合并/拣选某类内容」（框架 / 业务 / cursor 工具 / 指定提交）
- 用户说「同步框架」「同步 skills」「cherry-pick 某 commit」等

## 执行步骤

### 1. 确认当前分支与目标

- 执行 `git branch --show-current` 和 `git status -s`，确认**当前所在分支**及工作区是否干净（有未提交改动时先提示用户处理）。
- 根据用户描述确定：**从哪条分支取内容**、**取什么**（整分支合并 / 仅框架 / 仅业务 / 仅 cursor 工具 / 指定若干提交）。

### 2. 理解需求并选择策略

按用户意图选择一种主要方式（必要时组合使用）：

| 用户需求类型 | 典型场景 | 推荐策略 |
|--------------|----------|----------|
| 把 master 的框架同步到 thewartest | 当前在 thewartest，要让框架与 master 一致 | `git merge master`（若希望 thewartest 完全跟进 master 的框架与少量关键业务）；或只合并框架相关路径（见下方「按路径合并」） |
| 把 master 的 cursor 工具同步到 thewartest | 当前在 thewartest，只更新 .cursor 等 | 按路径合并：只从 master 拿 `.cursor/`、`.cursorrules`、`.gitignore` 等 |
| 把 thewartest 的某功能/提交合到 master | 当前在 master，要收纳业务或指定提交 | `git cherry-pick <commit>`（指定提交）或按路径/提交范围合并业务相关改动 |
| 把 thewartest 的框架相关修复合到 master | 当前在 master，thewartest 上修了框架 bug | `git cherry-pick <commit>`（仅拣选框架相关 commit）或从 thewartest 按路径合并框架目录 |

- **按路径合并**（只同步部分目录、不整分支 merge）：可用 `git checkout <源分支> -- <路径>` 把指定路径从源分支拿到当前分支，再由用户或本 skill 引导提交；若需更细粒度，可对多个路径分别执行后再一次性提交。
- **整分支合并**：`git merge <源分支>`，适合「当前分支要整体跟进另一分支」。
- **拣选提交**：`git cherry-pick <commit1> [commit2 ...]`，适合「只拿某几个提交」。

### 3. 整理 Pick 内容（必须）

- **必须**用文字整理并回复用户，包含：
  - **当前分支**、**源分支**；
  - **本次 pick 的范围**：是整分支、按路径（列出具体路径）、还是按提交（列出 commit hash 与简短说明）；
  - **拟执行的具体操作**（将运行的 git 命令或命令序列，如 `git merge master`、`git checkout master -- .cursor/`、`git cherry-pick abc123` 等）。
- 未整理并展示上述内容前，不得进入「比对差异」或执行任何写操作。

### 4. 比对分支代码差异（必须）

- **必须**在真正执行 merge / cherry-pick / checkout 前，对本次 pick 涉及的范围做差异比对，并把结果呈现给用户：
  - **整分支合并**：可用 `git log 当前分支..源分支 --oneline` 看源分支比当前分支多出的提交；可用 `git diff 当前分支..源分支 --stat` 看文件级差异概况。
  - **按路径合并**：对拟 checkout 的路径执行 `git diff 当前分支 源分支 -- <路径>` 或 `git diff 当前分支 源分支 --stat -- <路径>`，列出有差异的文件与行数概况。
  - **按提交拣选**：用 `git show <commit> --stat` 列出该提交改动的文件。
- 向用户说明：**是否存在差异**、**涉及哪些文件/提交**、**差异大致规模**（如「共 N 个文件、约 M 行变更」）。若比对结果为空（无差异），明确告知用户「当前分支与源分支在该范围内无差异，无需执行 pick」。

### 5. 二次确认（必须）

- 在整理完 pick 内容并完成差异比对后，**必须**向用户做**二次确认**，例如：
  - 「请确认是否按以上内容执行：当前分支 X，从分支 Y [merge/checkout/cherry-pick] [范围]，预计影响文件见上方差异。确认后我将执行具体命令。」
- **仅当用户明确表示同意**（如「确认」「可以执行」「同意」等）后，才可执行步骤 6 的实际 git 操作。
- 若用户未确认或表示取消/修改，则**不执行**任何 merge / cherry-pick / checkout，可根据用户新指示重新整理与比对。

### 6. 执行合并或拣选（仅在二次确认通过后）

- **仅在用户完成二次确认后**执行所选命令（如 `git merge`、`git cherry-pick`、`git checkout <分支> -- <路径>`）。
- 若出现冲突：提示用户冲突文件与位置，不自动解决业务逻辑冲突；可给出解决冲突的通用步骤（编辑、`git add`、`git cherry-pick --continue` 或 `git merge --continue`）。

### 7. 提交与反馈

- 若使用了 `git checkout <分支> -- <路径>`，需要再执行 `git add` 与 `git commit`，commit message 需符合项目规范（可结合 git-commit skill：标注【框架层】/【业务层】/【cursor 工具】）。
- 操作完成后简短反馈：当前分支、从哪条分支取了什么、执行了哪些命令、是否有冲突需人工处理。

## 按路径合并的常用范围

- **仅框架**：`slg-common/`、`slg-net/`、`slg-redis/`、`slg-support/`、`slg-table/`、根 `pom.xml`、各模块 `pom.xml`（按需列举）。
- **仅 cursor 工具**：`.cursor/`、`.cursorrules`、`.gitignore`（仅当确实包含 .cursor 相关修改时）。
- **仅业务**：`slg-game/`、`slg-scene/`、`slg-shared-modules/` 等（与 thewartest 业务相关路径）。

一次只对一个「范围」做路径合并，避免混入无关改动；若用户要同时同步多个范围，可拆成多次操作并分次提交。

## 注意事项

- **必须遵守**：用户提出 pick 要求后，**必须先整理 pick 内容、比对分支代码差异、并得到用户二次确认**，方可执行 merge / cherry-pick / checkout；不得跳过整理、比对或确认直接操作。
- **不执行**：`git push`、`rebase`、`force push`，除非用户明确要求。
- **不自动**：在未得到用户二次确认前，不执行任何会改写工作区或提交历史的命令；若有冲突，由用户决定最终内容后再 continue。
- **编码**：若需生成含中文的 commit message，使用 UTF-8 无 BOM 的临时文件 + `git commit -F`，避免乱码（与 git-commit skill 一致）。
