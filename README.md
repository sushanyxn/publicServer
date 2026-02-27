# SLG Server

SLG（策略类游戏）服务端项目，采用多进程架构：游戏服（slg-game）负责登录、玩家与养成逻辑，场景服（slg-scene）负责大地图、AOI 与场景实体，二者通过内部 RPC 与 WebSocket 通信。支持机器人（slg-robot）压测与协议模拟。

---

## 技术栈

| 类别     | 技术 / 版本 |
|----------|--------------|
| 语言     | Java 21      |
| 构建     | Maven        |
| 框架     | Spring Boot 3.3.0 |
| 网络     | WebSocket（服务端 + 客户端）、自定义 RPC（基于内部消息） |
| 配置表   | CSV + slg-support 表注解与加载 |
| 数据     | 实体缓存、MongoDB 持久化（slg-support） |
| 其他     | fastutil、Commons CSV、ClassGraph、CGLIB、SLF4J + Logback |

---

## 项目结构

```
slgserver/
├── slg-common    # 一级：公共组件（事件、进度、线程池、工具类、场景类型等）
├── slg-net       # 二级：网络层（协议定义与编解码、WebSocket、会话、RPC、消息注册与分发）
├── slg-support   # 二级：数据支撑（表配置注解与加载、实体、Table 管理）
├── slg-fight     # 三级：战斗业务（战斗结算、数值、战报 model→VO）
├── slg-game      # 三级：游戏逻辑（登录、玩家、场景调度、英雄/任务等养成、协议 Facade、RPC 路由）
├── slg-scene     # 三级：场景服（AOI、阵营、节点、场景实体与业务）
├── slg-robot     # 三级：机器人 / 压测客户端
└── table/        # CSV 配置表（英雄、任务、场景等）
```

### 模块职责简述

- **slg-common**：无模块依赖；提供事件总线、进度系统、执行器、通用工具与常量。
- **slg-net**：协议定义（clientmessage / innermessage）、编解码、WebSocket、消息注册（message.yml）、RPC 接口定义与调用。
- **slg-support**：表配置（@Table、CSV 加载）、实体缓存、Mongo 持久化与生命周期。
- **slg-fight**：战斗结算（FightSettlement）、战斗 model 转战报 VO，供 game、scene 调用。
- **slg-game**：客户端协议入口（Facade + @MessageHandler）、登录、玩家管理、英雄/任务养成、场景门面与 RPC 路由。
- **slg-scene**：场景创建与 AOI、阵营关系、场景节点（城市、军队、集结等）及节点组件（战斗、驻守、集结、战报等）。
- **slg-robot**：模拟客户端登录与协议发送，用于压测与联调。

### 进程入口

- 游戏服：`slg-game` → `GameMain.java`
- 场景服：`slg-scene` → `SceneMain.java`
- 机器人：`slg-robot` → `RobotMain.java`

### 依赖规则

- 一级 → 无依赖；二级 → 仅依赖 common；三级 → 依赖二级（game、scene 通过 support/net/fight 使用 common）。
- 禁止越级依赖（如 game/scene 不直接依赖 common）。

---

## 协议类型

协议在 `slg-net` 中定义，并在 `slg-net/src/main/resources/message.yml` 中注册（协议号全局唯一）。

### 命名约定

| 前缀   | 含义           | 示例                |
|--------|----------------|---------------------|
| CM_    | 客户端请求     | CM_LoginReq、CM_EnterScene、CM_GainTask |
| SM_    | 服务端推送/响应| SM_LoginResp、SM_SceneNodeAppear、SM_UpdateTask |
| IM_    | 内部消息       | IM_RpcRequest、IM_RegisterSessionRequest |
| *VO    | 数据体         | TaskVO、SceneNodeVO、FightHeroVO |

### 协议号分配

- `0`：null（系统保留）
- `1–50`：基础类型（系统保留）
- `100–999`：内部消息（rpc、socket 等），按模块预留 10–20 个
- `1000+`：客户端消息，按模块预留约 100 个（如 login 1000+、scene 1100+、task 1200+、army 1300+、hero 1400+、report 1500+）

### 当前已注册模块概览

| 模块   | 协议号范围   | 说明                     |
|--------|--------------|--------------------------|
| rpc    | 101–102      | RPC 请求/响应            |
| socket | 111–112      | 内部链接会话注册         |
| login  | 1001–1002    | 登录请求/响应            |
| scene  | 1100–1119    | 进入场景、加载完成、视野、节点出现/消失、各类 VO |
| task   | 1200–1203    | 领取任务奖励、任务更新、主线任务、TaskVO |
| army   | 1300–1304    | 军队/集结/世界 Boss 等 VO |
| hero   | 1400         | 英雄 VO                  |
| report | 1500–1513    | 战报及各模块 VO          |

客户端协议由 `slg-game` 中各模块的 **Facade** 处理（如 `LoginFacade`、`SceneFacade`、`TaskFacade`），方法使用 `@MessageHandler` 标注。

---

## 目前涉及的功能

### 游戏服（slg-game）

- **登录**：账号登录、会话绑定、登录响应（LoginFacade、LoginService）。
- **玩家**：Player 管理、PlayerManager、场景上下文（SceneContext）。
- **英雄养成**：英雄配置表（HeroTable、HeroLevelTable）、HeroManager/HeroService、HeroFacade。
- **任务**：主线任务配置（MainTaskTable）、任务进度、领取奖励（TaskFacade、TaskManager、TaskService）。
- **场景门面**：进入场景（CM_EnterScene）、加载完成（CM_LoadSceneFinish）、视野（CM_Watch），与场景服 RPC 协作。
- **内部消息与 RPC**：内部链接注册（InnerSocketFacade）、游戏服侧 RPC 路由（GameRpcRouteService）。

### 场景服（slg-scene）

- **场景与 AOI**：场景创建、网格 AOI（AoiController、MultiGridContainer）、视野与 Watcher。
- **阵营**：阵营类型与关系策略（玩家/联盟/NPC 等）。
- **场景节点**：静态节点（玩家城市、资源点等）、路径节点（行军中的军队）、节点所有者（玩家、联盟、怪物等）。
- **节点组件**：以组件方式扩展节点行为，例如：
  - 战斗（FightComponent）、驻守（GarrisonComponent）、交互（InteractiveComponent）、选目标（SelectTargetComponent）；
  - 玩家军队（行军、解散、战报）、集结（集结、解散、空闲）、玩家城市（交互、战报）；
  - 战报模块（属性、英雄、兵种、科技、成员、录像等 ReportModuleHandler）。
- **与游戏服协作**：通过 RPC（如 SceneRpcService）接收进入场景、加载完成等请求，推送节点出现/消失、军队出现/消失等。

### 战斗（slg-fight）

- **战斗结算**：基于 FightContext、FightArmy（英雄+兵种）的简单兵力对比结算（FightSettlement）。
- **战报**：战斗 model 转战报 VO（如 FightHeroVO、FightTroopVO），供场景/游戏服组包推送。

### 机器人（slg-robot）

- 连接游戏服 WebSocket、模拟登录与协议发送，用于压测与协议联调。

---

## 构建与运行

- 构建：在项目根目录执行 `mvn clean package`。
- 运行游戏服 / 场景服 / 机器人：运行对应模块的 `*Main` 类，或通过 Spring Boot 指定主类；配置见各模块 `src/main/resources/application.yml`。
- 数据库、Redis 等连接与敏感信息请通过配置文件管理，勿写入仓库。

---

## 其他说明

- 进度系统：进度类型在 `slg-common.progress.type`，条件实现 `IProgressCondition`，事件实现 `IProgressEvent`。
- 协议与 Facade 的对应关系、新增协议注册方式等，见项目根目录 `.cursorrules` 及 `.cursor/MEMORY.md`。
- 详细协议列表与说明可由 `.cursor/skills` 下的协议文档流程生成，输出在 `.cursor/protocol/`。
