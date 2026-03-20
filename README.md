# SLG Server

SLG（策略类游戏）服务端项目，采用多进程架构：游戏服（slg-game）负责登录、玩家与养成逻辑，场景服（slg-scene）负责大地图、AOI 与场景实体，二者通过内部 RPC 与 WebSocket 通信。RPC 支持 WebSocket 直连与 Redis Stream 路由（多 Game 服跨服）。支持单进程合并启动（slg-singlestart）与机器人（slg-robot）压测。

---

## 技术栈

| 类别     | 技术 / 版本 |
|----------|--------------|
| 语言     | Java 21      |
| 构建     | Maven        |
| 框架     | Spring Boot 3.5.12 |
| 网络     | WebSocket（服务端 + 客户端）、自定义 RPC（内部消息，支持直连 + Redis 路由） |
| 配置表   | CSV + slg-support 表注解与加载 |
| 数据     | 实体缓存、MongoDB 持久化（slg-support）、Redis（缓存 / RPC 路由） |
| 协调     | Zookeeper（Curator） |
| 其他     | fastutil、Commons CSV、ClassGraph、CGLIB、SLF4J + Logback |

---

## 项目结构

```
slgserver/
├── slg-common         # 一级：公共组件（事件总线、虚拟线程执行器、工具类、场景类型等）
├── slg-net            # 二级：网络层（协议编解码、WebSocket、RPC、SyncBus、跨服事件、消息注册）
├── slg-redis          # 二级：Redis 模块（缓存、排行榜，Spring Boot 自动配置）
├── slg-support        # 二级：数据支撑（表配置注解与加载、实体缓存、MongoDB/MySQL 持久化）
├── slg-shared-modules # 三级：共享模块（战斗、属性、进度等完整系统能力，供 game/scene 共用）
├── slg-game           # 三级：游戏逻辑（登录、玩家、英雄/任务养成、GM、场景调度、RPC 路由）
├── slg-scene          # 三级：场景服（AOI、阵营、节点、场景实体与节点组件）
├── slg-client         # 三级：客户端模拟器（JavaFX + Spring AI MCP Server，多账号管理、大地图）
├── slg-robot          # 三级：机器人 / 压测客户端
├── slg-log            # 三级：告警日志分析系统（Spring MVC + MySQL + ES + Spring Security）
├── slg-singlestart    # 四级：单进程合并启动（Game + Scene 同进程，开发/小规模部署）
├── slg-framework-test # 测试：底层框架集成/性能/压力测试（持久化、Redis 缓存、Redis Route 等）
├── table/             # CSV 配置表（英雄、任务、场景等）
└── docker/            # 本地依赖（Redis、Redis-Route、Zookeeper、MySQL、MongoDB、Elasticsearch）
```

### 模块职责简述

- **slg-common**：无模块依赖；提供事件总线、虚拟线程执行器（Executor / KeyedVirtualExecutor / GlobalScheduler）、Groovy 热更新、通用工具与常量。
- **slg-net**：协议定义（clientmessage / innermessage）、编解码、WebSocket、消息注册（message.yml）、RPC 接口与调用（直连 + Redis Stream 跨服路由）；SyncBus 跨进程实体字段同步、跨服事件传递。
- **slg-redis**：基于 Spring Boot 自动配置的 Redis 模块，提供字段级缓存（CacheAccessor / @CacheEntity / @CacheField）与排行榜服务；支持 standalone / cluster / sentinel。RPC 使用 Redis 路由时需独立 Redis 实例（见 docker/redis-route）。
- **slg-support**：表配置（@Table、CSV 加载）、实体缓存框架、数据库持久化（MongoDB @EnableMongo / MySQL @EnableMysql）与生命周期管理。
- **slg-shared-modules**：game 与 scene 共用的共享模块集合，含战斗结算（FightSettlement）、战报 model→VO、进度管理（ProgressManager、IProgressTable 等）以及通用属性容器系统。
- **slg-game**：客户端协议入口（Facade + @MessageHandler）、登录、玩家管理、英雄/任务养成、GM 系统、通知系统、场景门面；实现 IRpcRouteSupportService / IRouteSupportService，支持直连与 Redis 跨服 RPC。
- **slg-scene**：场景创建与 AOI、阵营关系、场景节点（城市、军队、集结等）及节点组件（战斗、驻守、集结、战报等）；RPC 路由适配与游戏服协作。
- **slg-client**：客户端模拟器，JavaFX UI + Spring Boot 后端 + Spring AI MCP Server；支持多账号管理、模块化 UI 面板（英雄、GM、登录等）、SLG 大地图渲染；MCP Server 暴露工具供 AI 自动化测试与造号。
- **slg-robot**：模拟客户端登录与协议发送，用于压测与联调。
- **slg-log**：告警日志分析系统，独立 Web 服务；基于 Spring MVC + MySQL(@EnableMysql) + Elasticsearch + Spring Security + JWT，提供日志搜索、统计分析和用户管理。
- **slg-singlestart**：四级模块，单进程启动 Game + Scene，共享 ServerId、RPC、数据库，便于开发或小规模部署。
- **slg-framework-test**：底层框架集成与性能测试模块；使用 Testcontainers（Redis/MySQL），覆盖持久化、Redis 缓存、Redis Route 的 E2E、性能与压力测试。

### 进程入口

- 游戏服：`slg-game` → `GameMain.java`
- 场景服：`slg-scene` → `SceneMain.java`
- 单进程（Game+Scene）：`slg-singlestart` → `SingleStartMain.java`
- 客户端模拟器：`slg-client` → `ClientMain.java`（JavaFX + Spring Boot，HTTP 端口 8099）
- 日志服：`slg-log` → `LogMain.java`（@EnableMysql，HTTP 端口 8092）
- 机器人：`slg-robot` → `RobotMain.java`

### 依赖规则

- **一级**（slg-common）→ 无依赖；**二级**（slg-net/redis/support）→ 仅依赖一级；**三级**（slg-game/scene/client/robot/log/shared-modules）→ 依赖二级；**四级**（slg-singlestart）→ 依赖三级（slg-game + slg-scene）。
- 禁止越级依赖（如 game/scene 不直接依赖 common，通过 support/net/shared-modules 间接使用）。

### RPC 路由方式

- **直连**：Game 与 Scene 通过 WebSocket 建立内部连接，RPC 请求按 serverId 发往对应连接；适合单 Game 或少量 Game 服。
- **Redis 路由**：通过独立 Redis（docker/redis-route）的 Stream 转发 RPC，Game 服无需与所有目标两两建连，适合多 Game 服、跨服调用。由 `slg-net` 的 RedisRoute、RpcRedisFacade 等提供，业务侧通过 `rpc.client.route-service-class` 等配置接入。

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
| event  | 201          | 跨服事件（战斗结算等）   |
| login  | 1001–1003    | 登录请求/响应/完成       |
| scene  | 1100–1119    | 进入场景、加载完成、视野、节点出现/消失、军队出现/消失、各类 VO |
| task   | 1200–1203    | 领取任务奖励、任务更新、主线任务、TaskVO |
| army   | 1300–1304    | 军队/集结/世界 Boss 等 VO |
| hero   | 1400–1403    | 英雄 VO、升级、信息推送、变更推送 |
| report | 1500–1513    | 战报及各模块 VO          |
| gm     | 1600–1601    | GM 指令请求/结果         |
| notify | 1700         | 服务端通知消息推送       |

客户端协议由 `slg-game` 中各模块的 **Facade** 处理（如 `LoginFacade`、`SceneFacade`、`TaskFacade`），方法使用 `@MessageHandler` 标注。

---

## 目前涉及的功能

### 游戏服（slg-game）

- **登录**：账号登录、会话绑定、登录响应、登录完成事件（LoginFacade、LoginService）。
- **玩家**：Player 管理、PlayerManager、场景上下文（SceneContext）。
- **英雄养成**：英雄配置表（HeroTable、HeroLevelTable）、HeroManager/HeroService、HeroFacade；支持英雄获取、升级、全量/增量推送。
- **任务**：主线任务配置（MainTaskTable）、任务进度、领取奖励（TaskFacade、TaskManager、TaskService）。
- **GM 系统**：GM 指令框架（IGMCommand 接口），支持动态获取英雄、设置等级等调试指令。
- **通知系统**：服务端通知消息推送（SM_NotifyMessage），客户端按 infoId 查 MessageTable 展示。
- **场景门面**：进入场景（CM_EnterScene）、加载完成（CM_LoadSceneFinish）、视野（CM_Watch），与场景服 RPC 协作。
- **内部消息与 RPC**：内部链接注册（InnerSocketFacade）；游戏服侧 RPC 路由（GameRpcRouteService）支持 WebSocket 直连与 Redis Stream 跨服路由。

### 场景服（slg-scene）

- **场景与 AOI**：场景创建、网格 AOI（AoiController、MultiGridContainer）、视野与 Watcher。
- **阵营**：阵营类型与关系策略（玩家/联盟/NPC 等）。
- **场景节点**：静态节点（玩家城市、资源点等）、路径节点（行军中的军队）、节点所有者（玩家、联盟、怪物等）。
- **节点组件**：以组件方式扩展节点行为，例如：
  - 战斗（FightComponent）、驻守（GarrisonComponent）、交互（InteractiveComponent）、选目标（SelectTargetComponent）；
  - 玩家军队（行军、解散、战报）、集结（集结、解散、空闲）、玩家城市（交互、战报）；
  - 战报模块（属性、英雄、兵种、科技、成员、录像等 ReportModuleHandler）。
- **与游戏服协作**：通过 RPC（如 SceneRpcService）接收进入场景、加载完成等请求，推送节点出现/消失、军队出现/消失等。

### 共享模块（slg-shared-modules）

- **战斗**：基于 FightContext、FightArmy（英雄+兵种）的简单兵力对比结算（FightSettlement）；战斗 model 转战报 VO（如 FightHeroVO、FightTroopVO），供场景/游戏服组包推送。
- **进度管理**：基于事件驱动的进度系统（ProgressManager、IProgressTable、IProgressCondition、IProgressEvent），供 game、scene 任务等使用；进度类型与转换器由各业务模块实现（如 GameProgressType、SceneProgressType）。
- **通用属性容器**：可扩展的属性系统（含读表与协议转化），供 game、scene 共用。

### 客户端模拟器（slg-client）

- **多账号管理**：支持同时登录多个账号，每个账号独立维护连接与状态。
- **模块化 UI 面板**：JavaFX 构建，按业务模块拆分面板（登录、英雄、GM、地图等），数据通过 IClientModule 绑定。
- **SLG 大地图**：MapCanvas 渲染场景节点、军队、城市等场景实体。
- **MCP Server**：基于 Spring AI MCP，暴露 login/disconnect/list_heroes/gm_command 等工具，供 Cursor AI 自动化测试与造号。

### 告警日志分析系统（slg-log）

- 独立 Web 服务，基于 Spring MVC + MySQL + Elasticsearch + Spring Security + JWT。
- 提供日志搜索、统计分析和用户管理功能。

### 机器人（slg-robot）

- 连接游戏服 WebSocket、模拟登录与协议发送，用于压测与协议联调。

---

## 框架特性

### 虚拟线程执行器

项目已完成从传统线程池到虚拟线程的统一迁移。所有模块共享底层 `KeyedVirtualExecutor`（按 key 串行、不同 key 并行）+ `GlobalScheduler`（全局定时调度），仅保留 2 个平台调度线程 + 1 个共享虚拟线程池。

- **多链模块**（PLAYER、SCENE_NODE、PERSISTENCE、ROBOT）：按 key 分链，`.join()` 安全。
- **单链模块**（SYSTEM、LOGIN、SCENE）：共用一条串行链，禁止 `.join()`，须用异步回调。

### SyncBus（跨进程实体同步）

位于 `slg-net` 的轻量级跨进程实体字段同步系统，通过 `@SyncEntity` + `@SyncField` 注解声明同步字段，底层复用 RPC 框架，支持限流与全量补偿。

### 跨服事件传递

业务事件实现 `ICrossServerEvent` 后，Bridge 自动将事件转换为 VO 并通过 RPC 转发到目标服务器。支持四种路由注解（`@RouteServer`、`@RoutePlayerGame`、`@RoutePlayerMainScene`、`@RoutePlayerCurrentScene`），fire-and-forget 模式，防循环转发。

### Redis 缓存

基于 Redis Hash 的字段级缓存读写（`CacheAccessor` / `@CacheEntity` / `@CacheField`），通过注解 + 枚举消除硬编码，支持字段级/整对象/批量读写。

### Groovy 热更新

`slg-common` 提供 `GroovyScriptManager`，支持运行时加载和替换 Groovy 脚本，实现不停服热更新。

---

## 构建与运行

- **构建**：在项目根目录执行 `mvn clean package`。
- **运行**：运行对应模块的 `*Main` 类（或 Spring Boot 指定主类）；配置见各模块 `src/main/resources/application.yml`。
- **测试**：单元测试按模块执行，如 `mvn test -pl slg-common`；底层框架集成与性能测试在 `slg-framework-test`，需 Docker（Testcontainers），执行 `mvn test -pl slg-framework-test`。测试计划与结果见 `.cursor/tests/plans` 与 `.cursor/tests/results`。
- **Docker 本地依赖**：`docker/` 下提供常用中间件编排，按需启动：
  - `docker/redis/`：通用 Redis
  - `docker/redis-route/`：RPC 跨服路由专用 Redis（多 Game 服时使用）
  - `docker/zookeeper/`：Zookeeper（含 init-zk.sh）
  - `docker/mysql/`、`docker/mongodb/`、`docker/elasticsearch/`：数据库与检索
- 数据库、Redis、Zookeeper 等连接与敏感信息请通过配置文件管理，勿写入仓库。

---

## 其他说明

- **进度系统**：进度管理在 `slg-shared-modules`（`com.slg.sharedmodules.progress`），条件实现 `IProgressCondition`，事件实现 `IProgressEvent`；业务进度类型在 `slg-game/core/progress`、`slg-scene/core/progress` 等。
- **协议文档**：详细协议列表与说明见 `.cursor/protocol/协议说明.md`（可通过 protocol-doc skill 重新生成）。
- **GM 指令文档**：GM 指令速查与详细用法见 `.cursor/gm/`（可通过 gm-scan skill 重新生成）。
- **项目规范与约定**：见 `.cursor/MEMORY.md` 及 `.cursor/rules/` 目录。
