---
description: 业务模块包结构与类放置规范，类必须放在对应职责的包下
globs:
  - "slg-game/**/*.java"
  - "slg-scene/**/*.java"
  - "slg-fight/**/*.java"
  - "slg-web/**/*.java"
  - "slg-log/**/*.java"
---

# 业务模块结构性要求

## 原则

**业务模块中的类必须放在与其职责对应的包下**，不得随意放置。新增类时参考本规范与项目现有包结构，保持与同模块、同子域下已有类一致。

## 包路径与类职责对应表

以下为各业务模块中**包路径模式**与**应放置的类类型/职责**的对应关系，按现有项目结构归纳。

### 通用模式（slg-game / slg-scene 等）

| 包路径模式 | 应放置的类类型 / 职责 |
|------------|------------------------|
| `com.slg.{module}` | 主类（XxxMain）、SpringContext、package-info |
| `com.slg.{module}.base.{子域}` | 该子域基础能力：entity、model、manager、service |
| `com.slg.{module}.base.{子域}.facade` | 协议入口 Facade，对应 message.yml，命名 `XxxFacade` |
| `com.slg.{module}.develop.{子域}` | 养成/发展玩法：facade、model、manager、service、table |
| `com.slg.{module}.develop.{子域}.event` | 该子域业务事件类：实现 IEvent 的具体事件（如 HeroLevelUpEvent），供 EventBus 发布与 @EventListener 消费 |
| `com.slg.{module}.bean.event` | 跨子域复用的事件接口/基类（如 IPlayerProgressEvent）；具体事件仍放在 develop.{子域}.event |
| `com.slg.{module}.scene`（game） | 场景相关：manager、table、facade、handler/impl、service |
| `com.slg.{module}.net` | 网络层：facade、handler、manager、rpc、工具类 |
| `com.slg.{module}.core.config` | 模块配置 Bean、中间件配置 |
| `com.slg.{module}.core.lifecycle` | 初始化、数据加载等生命周期类 |
| `com.slg.{module}.core.progress` | 进度相关类型、转换器 |
| `com.slg.{module}.bean.{类型}` | 通用扩展点接口/抽象类；`bean.{类型}.impl` 放具体实现 |

### 类类型命名与职责

| 包内常见后缀/类型 | 放置的类 | 职责 |
|------------------|----------|------|
| **Facade** | `XxxFacade` | 协议入口，接收客户端/内部协议，转调 service/manager |
| **Service** | `XxxService` | 无状态业务编排、领域服务 |
| **Manager** | `XxxManager` | 缓存/会话/实体集合/生命周期管理 |
| **model** 包 | 领域模型、DTO、上下文 | POJO、Context、Info 等 |
| **entity** 包 | `XxxEntity` | 持久化实体，对应 DB/存储 |
| **table** 包 | `XxxTable` | 配置表、静态表（如 CSV 配置） |
| **handler** / **impl** | `XxxHandler`、实现类 | 消息处理、场景分支、协议处理实现 |
| **rpc** 包 | RPC 路由/服务 | 内部服务间调用入口 |
| **config** 包 | 配置类 | Configuration、安全/中间件配置 |
| **lifecycle** 包 | 生命周期类 | 启动、数据加载（XxxInitLifeCycle 等） |
| **event** 包 | `XxxEvent`、`IXxxEvent` | 实现 IEvent 的业务事件类；子域事件放 develop.{子域}.event，通用事件接口放 bean.event |

### slg-scene 场景子域

| 包路径模式 | 应放置的类类型 / 职责 |
|------------|------------------------|
| `com.slg.scene.scene.base` | 场景实例：model、handler、manager、service、table、rpc |
| `com.slg.scene.scene.aoi` | AOI：model、service、tick |
| `com.slg.scene.scene.node.component` | 组件抽象、枚举；`impl.{子类型}` 下为具体 Component |
| `com.slg.scene.scene.node.node.model` / `impl` | 节点模型；impl 下为具体节点类型（如 PlayerCity、PlayerArmy） |
| `com.slg.scene.scene.node.owner` | 节点归属抽象（NodeOwner、NpcOwner 等） |
| `com.slg.scene.scene.camp.strategy` | 阵营关系策略接口及实现 |

### slg-web / slg-log（含 HTTP 的模块）

| 包路径模式 | 应放置的类类型 / 职责 |
|------------|------------------------|
| `com.slg.{module}.{功能域}.controller` | HTTP 接口，REST 入口 |
| `com.slg.{module}.{功能域}.service` | 该功能域业务服务 |
| `com.slg.{module}.{功能域}.entity` | 该功能域持久化实体 |
| `com.slg.{module}.{功能域}.dto` | 请求/响应 DTO（Request、Response） |
| `com.slg.{module}.{功能域}.repository` | 数据访问层 |
| `com.slg.{module}.auth` | 认证接口、上下文；`auth.impl` 实现；`auth.security` 为 Filter/Token 等 |
| `com.slg.{module}.config` | 全局配置、异常处理、安全配置 |
| `com.slg.{module}.response` | 统一响应、错误码 |
| `com.slg.{module}.utils` / `*.util` | 无状态工具类 |

### slg-fight

| 包路径模式 | 应放置的类类型 / 职责 |
|------------|------------------------|
| `com.slg.fight.wos` | 战斗/战报子域：入口接口（FightSettlement 等）、model（战斗上下文、兵种、记录等） |

## 实施要求

- **新增类**：先确定其职责（Facade/Service/Manager/model/entity/table/event/handler 等），再放入上表对应的包下；若存在同子域同类型包，与现有类放在同一包中。
- **新增子域**：参考同模块内已有子域（如 `base.login`、`develop.task`），保持 `facade`、`service`、`manager`、`model` 等子包命名一致。
- **跨层类**：若类同时涉及多职责，以**主要职责**归属包；必要时拆分为多个类分别放入对应包。
