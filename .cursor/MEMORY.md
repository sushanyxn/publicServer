# 项目记忆

本文件用于记录与本仓库相关的重要约定、决策和上下文，供 Cursor 助手在回答或修改代码时参考。

---

## 使用说明

- 在下方按分类添加需要长期保留的信息。
- 助手会在涉及相关主题时查阅本文件。
- 可随时增删改，保持简洁即可。

---

## 项目与架构

### 技术栈

- **语言**: Java 21
- **构建**: Maven，根目录 `pom.xml`
- **框架**: Spring Boot 3.3.0

### 模块与职责

| 模块 | 职责 |
|------|------|
| **slg-common** | 公共组件：事件、进度、虚拟线程执行器（Executor/KeyedVirtualExecutor/GlobalScheduler）、工具类、场景类型等 |
| **slg-net** | 网络层：协议定义与编解码、WebSocket、会话、RPC 框架、消息注册与分发 |
| **slg-redis** | Redis 模块：基于 Spring Boot 自动配置，提供缓存服务和排行榜服务。通过 Maven 依赖引入即自动启用，支持 standalone/cluster/sentinel |
| **slg-support** | 数据支撑：表配置注解与加载、实体缓存框架、数据库实现（MongoDB 等通过 `@EnableMongo` 按需引入）、Table 管理 |
| **slg-fight** | 战斗业务层（三级）：战斗结算、数值计算；战斗 model 转战报信息 VO（如 FightHero/FightTroop/FightArmy → FightHeroVO/FightTroopVO）；供 game、scene 调用 |
| **slg-game** | 游戏逻辑：登录、玩家、场景调度、英雄/任务等养成、协议 Facade、RPC 路由 |
| **slg-scene** | 场景服：AOI、阵营、节点、场景实体与业务处理 |
| **slg-robot** | 机器人/压测客户端 |
| **slg-web** | 导量服（Web 服务器）：客户端登录认证、game 服分配、账号管理、GM 后台管理，独立进程，基于 Spring MVC + MySQL(@EnableMysql) + Shiro + ZKConfig |
| **slg-log** | 告警日志分析系统：独立 Web 服务，基于 Spring MVC + MySQL(@EnableMysql) + ES + Spring Security + JWT，提供日志搜索、统计分析和用户管理 |
| **slg-singlestart** | 合并启动：将 Game 和 Scene 合并到同一进程运行，共享服务器ID、RPC服务和数据库 |

### 模块层级与依赖

- **一级基础包**：`slg-common`（无模块依赖）
- **二级支撑包**：`slg-net`、`slg-redis`、`slg-support`，仅依赖一级包（common）
- **三级业务包**：`slg-fight`、`slg-scene`、`slg-game`、`slg-robot`、`slg-web`、`slg-log`，依赖二级包
- **四级合并包**：`slg-singlestart`，依赖三级包（`slg-game` + `slg-scene`）

依赖规则：二级包依赖一级包，三级包依赖二级包；**禁止越级依赖**（如 scene/game/robot 不直接依赖 common，通过 support、net 或 fight 间接使用 common）。`slg-game`、`slg-scene` 均依赖 `slg-fight` 以使用战斗相关能力。战报所需的信息 VO（如 FightHeroVO、FightTroopVO）由 **slg-fight 的 model 提供转化方法**（如 `FightHero.toFightHeroVO()`、`FightArmy.toFightHeroVOs()`），不在外部重复转化。

### 进程入口

- **游戏服**: `slg-game/.../GameMain.java`
- **场景服**: `slg-scene/.../SceneMain.java`
- **合并服**: `slg-singlestart/.../SingleStartMain.java`（Game + Scene 同进程）
- **导量服**: `slg-web/.../WebMain.java`（@EnableMysql + @EnableRpcServer + @EnableZookeeper，HTTP 端口 8090，RPC 端口 8091）
- **日志服**: `slg-log/.../LogMain.java`（@EnableMysql，HTTP 端口 8092）
- **机器人**: `slg-robot/.../RobotMain.java`

### 关键路径

- 协议注册与扫描：`slg-net` 的 `message.yml` + `MessageRegistryInitializer` / `MessageHandlerInjector`
- 客户端协议处理：`slg-game` 中各模块的 `facade` 包下 `*Facade`，方法带 `@MessageHandler`
- 内部消息：`slg-net/.../innermessage/` 下各模块 `packet`，经 `GameServerMessageHandler` 等分发
- RPC 接口定义在 `slg-net/rpc/impl/`，实现与路由在 `slg-game`（如 `GameRpcRouteService`）、`slg-scene` 等

---

## 命名与约定

### 包与模块分类

- **slg-game** 下按业务分类：`base`（登录、玩家）、`develop`（英雄、任务等养成）、`scene`（场景门面与调度）、`net`（内部消息与 RPC 路由）、`core`（生命周期、进度转换）
- 新建类放在对应业务包下，避免堆在根包

### 协议类命名

- **客户端请求**: `CM_` + 操作名（如 `CM_LoginReq`、`CM_EnterScene`）
- **服务端推送/响应**: `SM_` + 操作名（如 `SM_LoginResp`、`SM_SceneNodeAppear`）
- **内部消息**: `IM_` + 操作名（如 `IM_RpcRequest`、`IM_RegisterSessionRequest`）
- **VO/数据结构**: 以 `VO` 结尾（如 `ScenePlayerVO`、`TaskVO`）

### Lombok 与代码精简

- **尽量使用 Lombok 减少代码量**：在符合可读性与项目规范的前提下，优先用 Lombok 替代手写样板代码。例如：DTO/VO/实体等用 `@Data` 或 `@Getter`/`@Setter`；构造器用 `@AllArgsConstructor`/`@NoArgsConstructor`/`@RequiredArgsConstructor`；Builder 用 `@Builder`；相等性用 `@EqualsAndHashCode` 等，避免手写大量 getter/setter、构造器、toString。

### 工厂方法命名

- **使用工厂方法构造对象时，方法名统一为 `valueOf`**（如 `FightArmy.valueOf(...)`、`MultiFightArmy.valueOf(...)`），不使用 `of`、`create` 等。

### 其他

- Facade：`模块名Facade`（如 `LoginFacade`、`HeroFacade`、`TaskFacade`、`SceneFacade`）
- 表配置类：`*Table`（如 `HeroTable`、`MainTaskTable`），放在各模块的 `table` 包
- 进度类型枚举在 `slg-common` 的 `progress.type` 包；业务进度类型在 `slg-game/core/progress` 等

---

## 协议与 message.yml

### 协议定义位置

- **客户端协议（与前端交互）**: `slg-net/.../message/clientmessage/模块名/packet/`
- **内部协议（进程间）**: `slg-net/.../message/innermessage/模块名/packet/`

所有 `packet` 包下的协议类都**必须**在 `message.yml` 中注册；协议号全局唯一，类名全局唯一。

### 协议号分配（message.yml）

- **0**: null（系统保留）
- **1–50**: 基础类型（系统保留）
- **100–999**: 内部消息（innermessage），按模块预留 10–20 个，如 rpc(101–110)、socket(111–120)
- **1000+**: 客户端消息（clientmessage），按模块预留约 100 个，如 login(1000–1099)、scene(1100–1199)、task(1200–1299)

### 配置文件

- **路径**: `slg-net/src/main/resources/message.yml`
- **格式**: 在 `messages:` 下按模块分组，每行 `协议号, 类名`

### 协议接收与处理（Facade）

- Facade 位置：`slg-game/.../模块分类/模块名/facade/`（如 `base/login/facade/LoginFacade`、`develop/task/facade/TaskFacade`）
- 使用 `@Component` 标注；处理方法用 `@MessageHandler` 标注
- **方法签名与强制约定**：
  - **两参**：`(NetSession session, 协议类 message)` — 仅在**主体未绑定阶段**使用，例如：登录、重连等尚未绑定 Player/主体的协议，以及内部协议、RPC、连接注册等非业务协议。
  - **三参**：`(NetSession session, 协议类 message, 操作主体 owner)` — 除上述未绑定阶段外，**所有业务协议必须使用三参、带主体的形式**，由框架注入当前玩家（或当前进程的等价主体）；框架根据参数个数自动识别（`needOwner = paramTypes.length >= 3`）。
- **第三参数（操作主体）约定**：
  - **游戏服（slg-game）**：第三参类型为 `Player`。框架通过 `session.getPlayerId()` 从 `PlayerManager` 取到 `Player` 后传入；若取不到则打错误日志且不调用 handler。
  - **机器人（slg-robot）**：第三参类型为 `Robot` 等该进程的“当前用户”对象（各进程可不同，见 `MessageHandlerMeta.needOwner` 注释）。
  - 仅当连接是**已注册玩家**（`session.getPlayerId() > 0`）时才会走三参分支；内部连接、RPC、登录中等均为两参调用。
  - 三参的 handler 会在**玩家虚拟线程链**中执行（`Executor.Player.execute(session.getPlayerId(), ...)`），两参的按消息类型在登录链、系统链或对应 TaskModule 链中执行。
- 一个 Facade 只处理本模块相关协议；协议类需有类注释说明用途。

---

## RPC 定义与使用

### 接口定义位置

- RPC 接口定义在 **slg-net**：`slg-net/.../rpc/impl/模块名/`，例如 `ISceneRpcService.java`
- 实现类在业务模块（如 slg-scene、slg-game）中实现该接口并注册为 Spring Bean

### 注解与约定

- 在**接口方法**上使用 `@RpcMethod` 表示可被远程调用
- **路由参数**：路由参数的**个数与类型**由当前使用的路由类（`@RpcMethod(routeClz = XxxRoute.class)`）中的 **`getRouteParams()` 方法返回值**决定。**`@RpcRouteParams` 的标注必须与使用的路由类的 `getRouteParams()` 返回值一致**（个数与类型均需一致，例如 `ServerIdRoute` 为 `[int.class]`，`PlayerCurrentSceneRoute` 为 `[long.class]`）。
- 线程键：用 `@ThreadKey` 标注参与路由/线程分派的参数（如 `long playerId`）
- 执行模块：`@RpcMethod(useModule = TaskModule.XXX)` 指定 RPC 方法在哪个 TaskModule 链中执行，默认 `TaskModule.PLAYER`
- 路由策略：`@RpcMethod(routeClz = XxxRoute.class)`，默认 `ServerIdRoute`；场景相关常用 `PlayerCurrentSceneRoute`
- 超时：`@RpcMethod(timeoutMillis = 毫秒)`，默认 30000

### 返回值

- `void`：单向调用（fire-and-forget），不等待返回
- `CompletableFuture<T>`：有返回值，业务层自行决定同步或异步消费

### RPC 回调不做线程回投

RPC 远程调用返回时，`future.complete()` / `completeExceptionally()` **直接在当前线程（网络 IO 线程或超时调度线程）执行**，不做线程回投。这样设计是为了避免死锁：若将 complete 操作投递回原 TaskKey 的线程链，而该线程正在 `join()` 等待此 Future，则 complete 操作永远排不到执行，形成死锁。

- **`join()` 模式**：`complete()` 在任意线程调用后，`join()` 会自动在原虚拟线程上恢复执行，线程安全由 drain 串行保证，无需额外分派。
- **`whenComplete()` / `thenApply()` 模式**：回调在 IO 线程上运行（Java 标准行为）。如果业务需要回到原线程链，需手动分派（如 `Executor.Player.execute(playerId, () -> { ... })`）。

### RPC 同步编程实践

项目基于虚拟线程，RPC 返回的 `CompletableFuture` 可以通过 `.join()` 同步等待结果而不阻塞平台线程。**但并非所有场景都适合同步等待**，必须根据任务所在链的类型判断：

#### 多链模块（MultiExecutor）可以使用 `.join()` 同步等待

多链模块（如 PLAYER、PERSISTENCE、ROBOT、SCENE_NODE）按 key 分链，同一 key 串行、不同 key 并发。`.join()` 阻塞的是当前 key 的虚拟线程链，**只影响当前 key 的后续任务**（如某个玩家），不影响其他 key，是安全的。

```java
// 正确：PLAYER 模块是多链，只阻塞当前玩家的链
try {
    int result = sceneRpcService.enterScene(serverId, playerId, sceneId).join();
    if (result == 0) {
        player.getSceneContext().updateScene(serverId, sceneId);
    }
} catch (CompletionException e) {
    LoggerUtil.error("[场景] 切图 RPC 异常", e.getCause());
}
```

#### 单链模块（SingleExecutor）禁止使用 `.join()` 同步等待

单链模块（如 SYSTEM、LOGIN、SCENE）所有任务共用一条串行链。如果在单链中 `.join()` 等待 RPC 返回，**会阻塞整条链上所有后续任务**，导致该模块所有业务停滞。例如在 SCENE 链中等待会导致所有场景任务排队等待，在 LOGIN 链中等待会导致所有登录请求排队。

```java
// 错误：SCENE 模块是单链，.join() 会阻塞所有场景任务！
Executor.Scene.execute(() -> {
    int result = someRpc.doSomething(...).join(); // 禁止！整条链被阻塞
});

// 正确：单链模块使用异步回调，回调在 IO 线程执行，需手动切回业务线程
Executor.Scene.execute(() -> {
    someRpc.doSomething(...).whenComplete((result, error) -> {
        // 回调在 IO 线程执行，需手动切回 SCENE 链
        Executor.Scene.execute(() -> handleResult(result, error));
    });
});
```

#### 判断规则

| 当前执行链 | `.join()` 同步 | `.whenComplete()` 异步 |
|-----------|---------------|----------------------|
| 多链（PLAYER 等） | **推荐**，安全且线程安全 | 可用，回调在 IO 线程执行，需手动切回业务线程 |
| 单链（SYSTEM/LOGIN/SCENE） | **禁止**，会阻塞整条链 | 必须使用，回调在 IO 线程执行，需手动切回业务线程 |

### 内部协议

- RPC 请求/响应使用内部消息：`IM_RpcRequest`、`IM_RpcRespone`，在 `message.yml` 的 rpc 模块中已注册（101、102）

### 路由与调用方

- 游戏服通过 `slg-game/net/rpc/GameRpcRouteService` 等做路由；场景服实现 `ISceneRpcService` 等接口，按 serverId 或玩家当前场景路由到对应进程

---

## 执行器架构（虚拟线程）

### 总体架构

项目已完成从传统线程池到虚拟线程的统一迁移。所有模块共享底层的 `KeyedVirtualExecutor`（带 key 的串行执行器）+ `GlobalScheduler`（全局定时调度器），仅保留 2 个平台调度线程 + 1 个共享虚拟线程池，取代了此前各模块各自创建的 `ScheduledExecutorService`。

### 核心组件（位于 `slg-common` 的 `com.slg.common.executor` 包）

| 组件 | 职责 |
|------|------|
| **KeyedVirtualExecutor** | 按 `TaskKey(module, id)` 分链串行执行任务，不同 key 可并行，使用虚拟线程 |
| **GlobalScheduler** | 全局定时调度器，内部使用 2 个平台线程的 `ScheduledExecutorService`，到时后将任务投递到 `KeyedVirtualExecutor` |
| **VirtualExecutorHolder** | 持有共享的虚拟线程 `ExecutorService`（`newVirtualThreadPerTaskExecutor`） |
| **TaskModule** | 枚举，定义任务模块类型，含 `multiChain` 属性区分多链/单链；`getName()` 首字母大写后对应 `Executor` 中的静态字段名 |
| **MultiExecutor** | 多链模块执行器，封装 `KeyedVirtualExecutor` + `GlobalScheduler`，所有方法需传入 `key` 参数（如 playerId） |
| **SingleExecutor** | 单链模块执行器，封装 `KeyedVirtualExecutor` + `GlobalScheduler`，该模块所有任务共用一条串行链，不接受 `key` 参数 |
| **Executor** | Spring 组件，持有各模块的 `MultiExecutor` / `SingleExecutor` 静态字段；`init()` 通过反射遍历 `TaskModule` 自动创建并赋值，新增 `TaskModule` 后只需在 `Executor` 中添加对应类型的静态字段即可，启动时会双向校验 |

### 模块类型

| TaskModule | 链类型 | Executor 字段 | 说明 |
|------------|--------|---------------|------|
| PLAYER | 多链 | `Executor.Player` (MultiExecutor) | 按 playerId 分链，同一玩家串行 |
| SCENE_NODE | 多链 | `Executor.SceneNode` (MultiExecutor) | 按 nodeId 分链，同一节点串行 |
| SYSTEM | 单链 | `Executor.System` (SingleExecutor) | 所有系统任务串行 |
| LOGIN | 单链 | `Executor.Login` (SingleExecutor) | 所有登录任务串行 |
| SCENE | 单链 | `Executor.Scene` (SingleExecutor) | 所有场景任务串行 |
| PERSISTENCE | 多链 | `Executor.Persistence` (MultiExecutor) | 按实体 ID 分链，同一实体串行 |
| ROBOT | 多链 | `Executor.Robot` (MultiExecutor) | 按 robotId 分链，同一机器人串行 |

### 调用方式

```java
// 多链模块（按 key 隔离）— MultiExecutor，必须传 key
Executor.Player.execute(playerId, () -> { ... });
Executor.Persistence.execute(entityId, () -> { ... });
Executor.SceneNode.execute(nodeId, () -> { ... });

// 单链模块 — SingleExecutor，不传 key
Executor.System.execute(() -> { ... });
Executor.Scene.execute(() -> { ... });

// 定时任务
Executor.Scene.schedule(() -> { ... }, delay, TimeUnit.MILLISECONDS);
Executor.Player.scheduleWithFixedDelay(playerId, () -> { ... }, initialDelay, delay, TimeUnit.MILLISECONDS);
```

### 已删除的旧类

- `slg-game`/`slg-scene`/`slg-robot` 各自的 `Executor`、`PlayerExecutorService`、`SystemExecutorService`、`LoginExecutorService`、`SceneExecutorService`、`RobotExecutorService`
- `slg-net` 的 `RpcExecutor`、`RpcExecutorService`、`RpcThread`
- `slg-common` 的 `IMultiExecutor`、`ISingleExecutor`
- `slg-support` 的 `PersistenceThreadPool`、`RetryableTask`（重试逻辑提取为 `PersistenceRetryWrapper`）

### 注意事项

- 禁止在业务代码中自行创建传统线程池
- 虚拟线程中禁止使用 `synchronized`（会 pin 载体线程），应使用 `ReentrantLock`
- 场景 Tick 等需要"完成后再延迟"语义的定时任务，使用自递归调度模式
- RPC 分派已改为 `@RpcMethod(useModule = TaskModule.XXX)` + `KeyedVirtualExecutor` 直达，不再经过 `RpcExecutor` 中间层
- 新增 `TaskModule` 枚举值后，只需在 `Executor` 类中添加对应类型（`MultiExecutor` / `SingleExecutor`）的 `public static` 字段，字段名 = `TaskModule.getName()` 首字母大写；`init()` 会自动通过反射完成初始化，缺失或类型不匹配时启动即抛异常

---

## 日志输出规范

- **统一使用 LogUtil 工具类**进行日志输出，**不允许使用 slf4j**。
- **避免重复输出多行 debug 日志**；对于正常流程，选取关键信息进行 **info** 级别输出；对于错误日志则使用 **error** 级别输出。

---

## 环境与配置

- 各可运行模块的配置在对应 `src/main/resources/application.yml`（如 slg-game、slg-scene）
- 数据库、Redis 等连接与敏感信息通过配置文件管理，不要硬编码到仓库

---

## 同步总线（SyncBus）

### 概述

SyncBus 是位于 `slg-net` 的 `com.slg.net.syncbus` 包下的轻量级跨进程实体字段同步系统，职责为"把 Holder 端指定字段的值传送到 Cache 端"。底层复用 RPC 框架，本地调用零网络开销，远程调用走 WebSocket。

### 核心概念

| 概念 | 说明 |
|------|------|
| **SyncModule** | 同步模块枚举（显式 ID），位于 `syncbus/SyncModule`，如 `PLAYER(1)` |
| **ISyncHolder** | 数据源端接口（如 Game 的 `PlayerEntity`），提供 `getSyncId()` 和 `syncTargetServerIds()` |
| **ISyncCache** | 数据接收端接口（如 Scene 的 `ScenePlayerEntity`），提供 `getSyncId()` 和可选回调 `onSyncUpdated()` |
| **@SyncEntity** | 类注解，声明所属 SyncModule，如 `@SyncEntity(SyncModule.PLAYER)` |
| **@SyncField** | 字段注解，标记参与同步的字段。两端字段名必须相同 |
| **ISyncCacheResolver** | Cache 端查找器，每种 Cache 实体实现一次，用于按 syncId 查找 Cache 实体 |

### 编解码

- 默认使用 `JsonUtil.toJson()` / `JsonUtil.fromJson()` 序列化
- 两端 model 不同时：Holder 端用 `@SyncField(encoder = XxxEncoder.class)`，Cache 端用 `@SyncField(decoder = XxxDecoder.class)`
- Encoder 实现 `ISyncFieldEncoder<T>`，Decoder 实现 `ISyncFieldDecoder<T>`，各自定义在自己的模块中

### 限流机制

- `@SyncField(syncInterval = N)`：N 秒内同一字段最多实际发送一次 RPC，默认 1 秒
- `syncInterval = 0`：不限流，每次 `sync()` 立即发送
- 限流采用 dirty 标记 + 定时器自递归自清理模式，无需业务手动管理

### 业务调用

```java
// 单字段同步（受限流控制）
playerEntity.setAllianceId(newAllianceId);
SyncBus.sync(playerEntity, PlayerEntity.Fields.allianceId);

// 全量同步（不经过限流，立即发送所有 @SyncField 字段）
SyncBus.syncAll(playerEntity);

// 清理限流状态（实体生命周期结束时调用，如玩家下线）
SyncBus.remove(playerEntity.getSyncId());
```

### 注意事项

- `SyncBus.sync()` 和 `SyncBus.remove()` 必须在对应实体的执行器链内调用
- fire-and-forget 模式，不保证强一致性；关键节点可用 `syncAll()` 全量补偿
- 两端字段名对齐由开发者保证，不匹配时运行时 error 日志
- 每个 SyncModule 在同一进程内只有一个 Holder 类和一个 Cache 类

---

## 跨服事件传递

### 概述

位于 `slg-net` 的 `com.slg.net.crossevent` 包下，对 `EventBusManager` 零侵入。业务事件实现 `ICrossServerEvent` 后，Bridge 自动将事件转换为 VO 并通过 RPC 转发到目标服务器，接收端以本地事件形式发布。

### 接入步骤

**1. 定义事件 VO**（位于 `slg-net/.../innermessage/event/packet/`）：

```java
@Getter @Setter
public class HeroLevelUpEventVO implements IEvent {
    @RoutePlayerGame   // 路由注解：发往玩家所在的 Game 服
    private long playerId;
    private int heroId;
    private int level;
}
```

- VO 必须实现 `IEvent`，位于 `com.slg.net.message` 包下
- VO 必须在 `message.yml` 中注册（协议号建议 121-150）
- 每个 VO 有且仅有一个路由注解字段

**2. 业务事件实现 `ICrossServerEvent`**：

```java
public class HeroLevelUpEvent implements IPlayerProgressEvent, ICrossServerEvent {
    // ... 业务字段 ...

    @Override
    public HeroLevelUpEventVO toCrossEvent() {  // 必须用协变返回类型
        HeroLevelUpEventVO vo = new HeroLevelUpEventVO();
        vo.setPlayerId(player.getId());
        // ... 填充字段 ...
        return vo;
    }
}
```

- `toCrossEvent()` 必须声明具体 VO 类型（协变返回类型），不能写 `IEvent`
- 返回 `null` 表示本次不需要跨服传播

**3. 接收端监听 VO 类型**：

```java
@EventListener
public void onHeroLevelUp(HeroLevelUpEventVO event) {
    // 在目标服务器处理事件
}
```

### 四种路由注解

| 注解 | 标注字段类型 | 路由策略 | 接收端 TaskModule |
|------|-------------|---------|------------------|
| `@RouteServer` | `int` (serverId) | ServerIdRoute | SYSTEM |
| `@RoutePlayerGame` | `long` (playerId) | PlayerGameRoute | PLAYER |
| `@RoutePlayerMainScene` | `long` (playerId) | PlayerMainSceneRoute | PLAYER |
| `@RoutePlayerCurrentScene` | `long` (playerId) | PlayerCurrentSceneRoute | PLAYER |

### 防循环转发

使用 ThreadLocal 防重入标记。RPC 接收端发布事件前设标记，Bridge 监听器检测到标记后跳过转发，防止循环。

### 注意事项

- Bridge 监听器优先级 `Integer.MAX_VALUE`（最低），本地监听器先执行
- 所有跨服事件为 fire-and-forget，不等待返回
- VO 路由元数据启动时预构建，运行时零反射
- 启动时校验 `toCrossEvent()` 返回类型，不合格则启动失败

---

## 合并启动（slg-singlestart）

### 概述

`slg-singlestart` 是四级模块，依赖 `slg-game` + `slg-scene`，将两者合并到**同一个 JVM、同一个 Spring 上下文**中运行。设计原则：不修改 game/scene 的业务逻辑，所有兼容处理封装在 singlestart 模块内。

### 核心设计

- **共享服务器 ID**：`server.game.server-id` 和 `server.scene.server-id` 均为 1，`bind-scene-id` 也为 1
- **单 RPC 服务**：只启动一个 RPC 端口（51001），所有 RPC 调用通过 `isLocal()` 判断为本地调用，直接走方法调用，不经过网络
- **单数据库**：Game 和 Scene 共用同一个 MongoDB 数据库（`slg_singlestart`）
- **统一路由**：`SingleRpcRouteService` 替代 `GameRpcRouteService` / `SceneRpcRouteService`，`isLocal()` 恒返回 true

### Bean 冲突处理

Game 和 Scene 存在同名 Bean，通过两种方式解决：

**显式命名**（修改 game/scene 的 `@Component` 名）：

| 原默认名 | Game 新名 | Scene 新名 |
|---------|----------|-----------|
| `springContext` | `gameSpringContext` | `sceneSpringContext` |
| `innerSessionManager` | `gameSessionManager` | `sceneSessionManager` |
| `mainSceneHandler` | `gameMainSceneHandler` | （默认名，无冲突） |

**excludeFilters 排除**（在 `SingleStartMain` 的 `@ComponentScan` 中排除）：

| 排除的类 | 原因 |
|---------|------|
| `GameMain` / `SceneMain` | 避免其 `@SpringBootApplication` / `@EnableRpcServer` 等注解被重复导入 |
| `GameInnerRequestFacade` | `@MessageHandler` 与 `SceneInnerRequestFacade` 处理同一消息类型冲突 |
| `SceneInnerResponseFacade` | `@MessageHandler` 与 `GameInnerResponseFacade` 处理同一消息类型冲突 |
| `SceneClientMessageHandler` | 与 Game 端的 `webSocketClientMessageHandler` 显式同名冲突 |

### 生命周期

`SingleInitLifeCycle`（phase = `SINGLE_INIT`，在 `GAME_INIT` 和 `SCENE_INIT` 之后）：
- 将本地 Scene 的 `ConnectState` 设为 `READY`（因 `connectServer()` 检测到同服直接返回，不走 WebSocket 握手）
- 触发 `batchInitPlayerScene()` 完成玩家场景初始化

### 数据持久化

关停时的实体保存逻辑统一在 `CacheFlushLifeCycle.stop()`（`slg-support`）中执行，`GameDataLoadingLifeCycle` 和 `SceneDataLoadingLifeCycle` 的 `stop()` 不做保存。无论单进程还是合并进程，保存只执行一次。

### 数据库与中间件选择

数据库和中间件按如下方式引入：
- `@EnableMysql`：MySQL 数据库，定义在 `slg-support`，依赖 `spring-boot-starter-data-jpa` + `mysql-connector-j`（均 optional），使用模块需在 `pom.xml` 显式引入 JPA 和 MySQL 驱动，并在启动类标注 `@EnableMysql`
- `@EnableMongo`：MongoDB 数据库，定义在 `slg-support`，依赖 `spring-boot-starter-data-mongodb`（optional），在启动类标注注解启用
- `slg-redis`：Redis 缓存和排行榜，独立模块，通过 Maven 依赖 `slg-redis` 引入即自动启用（基于 Spring Boot 自动配置），无需注解
- `@EnableZookeeper`：Zookeeper 配置读取和信息共享，定义在 `slg-net.zookeeper`，依赖 `curator-recipes`（optional），在启动类标注注解启用

### Redis 模块（`slg-redis`）

- 包位置：`slg-redis/src/main/java/com/slg/redis/`
- **独立二级模块**，依赖 `slg-common`，通过 Spring Boot 自动配置注册 Bean
- 配置前缀：`spring.data.redis`（标准 Spring Boot 配置），支持 standalone / cluster / sentinel
- 集群拓扑刷新：通过 `spring.data.redis.lettuce.cluster.refresh.adaptive=true` 和 `period` 配置即可
- 配置类：`SlgRedisConfiguration`（`@Configuration`），仅定义自定义 `RedisTemplate` Bean；其余组件（`RedisConnectionValidator`、`RedisLifeCycleConfiguration`）均为 `@Component`，通过 ComponentScan 加载
- **禁止直接操作 RedisTemplate**：所有 Redis 缓存相关业务必须通过 `CacheAccessor` 进行，不允许在业务代码中直接注入或使用 `RedisTemplate`/`StringRedisTemplate`
- 生命周期阶段：`LifecyclePhase.REDIS`（在 DATABASE 之后）
- 序列化：Key 使用 String，Value 使用 Jackson JSON（基于 JsonUtil 的 ObjectMapper）
- 引入方式：模块 `pom.xml` 中添加 `<dependency>slg-redis</dependency>` 即可，无需 `@Enable` 注解

### Redis 缓存包（`com.slg.redis.cache`）

- 包位置：`slg-redis/src/main/java/com/slg/redis/cache/`
- 基于 Redis Hash 实现复杂对象的**字段级缓存读写**，通过注解 + 枚举消除硬编码
- Redis Key 格式：`cache:{modulePrefix}:{entityId}`（如 `cache:player:10001`）
- **核心组件**：
  - `CacheModule`：缓存模块枚举（显式 id + keyPrefix），新增模块只需添加枚举值
  - `@CacheEntity`：类注解，声明所属 CacheModule（每个 Module 对应一个实体类）
  - `@CacheField`：字段注解，标记参与缓存的字段，可指定自定义 Codec
  - `CacheAccessor<T>`：泛型缓存访问器，提供字段级/整对象/批量读写
  - `@CacheAccessorInject`：注入注解，按泛型参数自动注入对应的 CacheAccessor
  - `CacheMetaRegistry`：启动时扫描 @CacheEntity 类构建元数据
  - `CacheAccessorManager`：管理 Accessor 实例 + BeanPostProcessor 处理注入
- **编解码**：默认 `JsonCacheFieldCodec`（基本类型直接 String.valueOf 优化，复杂类型 JSON），可通过 `@CacheField(codec = XxxCodec.class)` 自定义
- **批量查询**：
  - `batchGetAll(entityIds)` / `batchGet(entityIds, fieldNames...)` 批量获取多个实体
  - Standalone/Sentinel 使用 Pipeline，Cluster 使用 Lettuce 原生异步命令（自动路由到正确节点）
  - `get(entityId, fieldNames...)` 选择性加载指定字段，未指定字段保持 Java 默认值
- **加载方式**：所有类使用 `@Component`，通过 ComponentScan（`com.slg.redis`）加载，无 AutoConfiguration
- **使用示例**：
  ```java
  @CacheEntity(module = CacheModule.PLAYER)
  @FieldNameConstants
  @Getter @Setter
  public class PlayerCacheObj {
      @CacheField private String name;
      @CacheField private int level;
  }

  // 注入
  @CacheAccessorInject
  private CacheAccessor<PlayerCacheObj> playerCache;

  // 使用
  playerCache.setField(playerId, PlayerCacheObj.Fields.name, "张三");
  PlayerCacheObj obj = playerCache.getAll(playerId);
  Map<Object, PlayerCacheObj> batch = playerCache.batchGetAll(playerIds);
  ```

### Zookeeper 模块（`com.slg.net.zookeeper`）

- 包位置：`slg-net/src/main/java/com/slg/net/zookeeper/`
- 配置前缀：`zookeeper`
- 客户端：Apache Curator Framework（ExponentialBackoffRetry 重试策略）
- 核心服务：
  - `ZookeeperConfigService`：底层配置节点读写、CuratorCache 监听
  - `ZookeeperShareService`：服务器注册信息读写、instance 临时节点管理
- 生命周期阶段：`LifecyclePhase.ZOOKEEPER`（在 TABLE_CHECK 之后、DATABASE 之前）
- namespace 使用 `basePath` 配置项（默认 `/slg`），所有路径操作相对于此 namespace

### ZK 信息模型

- 路径常量：`ZkPath`（`com.slg.net.zookeeper.constant`），定义所有节点路径名
- 存储规则：每个配置项是独立的 ZK 子节点（非 JSON 整体），Redis/ 和 MongoDB/ 为二级子树
- 模型类（`com.slg.net.zookeeper.model`）：
  - `GameServerZkInfo`：GameServer 完整注册信息（网络、RPC、状态、导量、Redis、MongoDB、alive）
  - `SceneServerZkInfo`：SceneServer 注册信息（RPC、bind_game_id、状态、Redis、MongoDB、alive）
  - `RedisZkInfo` / `MongoZkInfo`：数据库连接子结构
  - `ServerType`：枚举区分 GAME / SCENE，持有各自的 basePath 和 configEndFlag
- instance 临时节点（EPHEMERAL）：进程启动时创建，断开连接时 ZK 自动销毁，用于存活检测
- ZK 节点树结构：
  - `/GameServers/{serverId}/` — game_ip, game_host, game_port, rpc_ip, rpc_port, enable, inServerList, openTimeMs, registedRole, dbVersion, timeZoneOffset, mergeServerVersion, diversion_config, diversion_switch, multiRoleServerShow, GAME_CONFIG_END_FLAG, instance, Redis/{host,port,password}, MongoDB/{db_name,url}
  - `/SceneServers/{serverId}/` — rpc_ip, rpc_port, bind_game_id, enable, dbVersion, timeZoneOffset, SCENE_CONFIG_END_FLAG, instance, Redis/{host,port,password}, MongoDB/{db_name,url}

### ZK 监听增量更新机制

- 监听回调采用**增量更新**策略，避免每次节点变化都全量重读所有字段
- CuratorCache 回调中已携带精确的变化路径（path）和新值（data），`ZookeeperShareService` 从 path 中解析出 serverId 和 fieldName
- **普通字段**：直接用回调中的 data 更新 `GameServerZkInfo` / `SceneServerZkInfo` 的对应属性（通过 `updateField(field, data)` 方法，内部使用 switch 匹配 ZkPath 常量），零 ZK 读取
- **Redis/MongoDB 子树**：解析到 fieldName 为 `"Redis"` 或 `"MongoDB"` 时，回退为重读对应子树（2-3 次 ZK 读取），而非全量 reload
- **全量 reload 仅在以下场景触发**：本地无缓存的新 server 加入、serverId 根节点新增
- **删除事件**：fieldName 为 null 且 data 为 null 时，表示 serverId 根节点被删除，直接从 ZKConfig 中移除
- 日志抑制：ZK 生命周期启动时通过 Logback API 将 `org.apache.zookeeper` 和 `org.apache.curator` 日志级别设为 WARN，封装在 `slg-net` 内部，引入模块自动生效

---

## MySQL 框架规范（`@EnableMysql`）

### 概述

项目通过 `slg-support` 提供统一的 MySQL 持久化框架，基于 JPA（EntityManager）+ EntityCache 实现。所有使用 MySQL 的模块**必须**遵循此框架，**禁止**自行使用 `JpaRepository`（Spring Data JPA Repository 接口）或直接操作 `EntityManager`。

### 接入步骤

**1. pom.xml 依赖**：

```xml
<!-- 内部模块：slg-support 提供通用数据库框架 -->
<dependency>
    <groupId>com.yxn.server</groupId>
    <artifactId>slg-support</artifactId>
</dependency>
<!-- MySQL + JPA（配合 @EnableMysql 使用） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

**2. 启动类**：

```java
@SpringBootApplication
@EnableMysql
@ComponentScan(basePackages = {
    "com.slg.entity",           // EntityCache、BaseMysqlRepository 等框架组件
    "com.slg.xxx",              // 本模块包
    "com.slg.common.executor"   // GlobalScheduler（EntityCache 依赖）
})
public class XxxMain { ... }
```

**3. 实体定义**：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "表名")
@CacheConfig(maxSize = -1, expireMinutes = -1)  // 小表全量缓存
public class XxxEntity extends BaseMysqlEntity<ID类型> {
    // 自增主键需覆写 getId() 添加 @GeneratedValue
    // 业务字段使用 @Column 注解
    // 复杂类型使用 @Serialized + @Column(columnDefinition = "json")

    @Override
    public void save() {
        XxxService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        XxxService.getInstance().saveField(this, fieldName);
    }
}
```

**4. Service 使用 EntityCache**：

```java
@Service
public class XxxService {
    @Getter
    private static XxxService instance;

    @EntityCacheInject
    private EntityCache<XxxEntity> xxxCache;

    @PostConstruct
    private void init() {
        instance = this;
        xxxCache.loadAll();  // 小表全量加载
    }

    public XxxEntity findById(Object id) { return xxxCache.findById(id); }
    public XxxEntity create(XxxEntity entity) { return xxxCache.insert(entity); }
    public XxxEntity save(XxxEntity entity) { return xxxCache.save(entity); }
    public void saveField(XxxEntity entity, String field) { xxxCache.saveField(entity, field); }
    public void delete(Object id) { xxxCache.deleteById(id); }
    public List<XxxEntity> findAll() { return new ArrayList<>(xxxCache.getAllCache()); }
}
```

### 核心组件

| 组件 | 包位置 | 职责 |
|------|--------|------|
| `@EnableMysql` | `com.slg.entity.mysql.anno` | 启用注解，导入 `MysqlConfiguration` + `MysqlLifeCycleConfiguration` |
| `BaseMysqlEntity<ID>` | `com.slg.entity.mysql.entity` | MySQL 实体基类，继承 `BaseEntity`，通过 `@Access(PROPERTY)` 在 getter 上标注 JPA 注解 |
| `BaseRepository` | `com.slg.entity.db.repository` | 通用仓储接口，定义 CRUD 操作 |
| `BaseMysqlRepository` | `com.slg.entity.mysql.repository` | MySQL 仓储实现（基于 `EntityManager`），由 `@EnableMysql` 自动引入 |
| `EntityCache<T>` | `com.slg.entity.cache.model` | 实体缓存，缓存优先的数据访问（Caffeine + 异步持久化 + Write-Behind） |
| `@EntityCacheInject` | `com.slg.entity.cache.anno` | 自动注入 `EntityCache` 实例 |
| `@CacheConfig` | `com.slg.entity.cache.anno` | 实体类注解，配置缓存参数（maxSize、expireMinutes、writeDelay 等） |
| `@Serialized` | `com.slg.entity.mysql.anno` | 字段注解，自动将复杂类型以 JSON 格式存储到 MySQL |
| `AsyncPersistenceService` | `com.slg.entity.db.persist` | 异步持久化服务，统一的数据库操作入口 |

### 关键约定

- **禁止使用 `JpaRepository`**：不允许定义 `extends JpaRepository<T, ID>` 的接口，所有数据访问通过 `EntityCache` 或 `BaseRepository`
- **禁止直接操作 `EntityManager`**：业务代码中不允许直接注入或使用 `EntityManager`
- **实体必须继承 `BaseMysqlEntity<ID>`**：提供统一的 id、createTime、updateTime 字段
- **实体必须实现 `save()` 和 `saveField()`**：通过 Service 的静态单例 `getInstance()` 回调到 `EntityCache`
- **主键选择**：业务含义主键（如 username）优先使用 String 类型；自增主键使用 Long + `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- **已使用 `@EnableMysql` 的模块**：`slg-web`、`slg-log`

---

## 其他

- 进度系统：进度类型在 `slg-common.progress.type`；条件实现 `IProgressCondition`，事件实现 `IProgressEvent`；`ProgressMeta` 序列化后需通过 `IProgressTypeTransform` 恢复 type
- 表数据：CSV 放在项目根下 `table/` 目录，由 slg-support 的 Table 体系加载

---

*最后更新：2026-02-26*
