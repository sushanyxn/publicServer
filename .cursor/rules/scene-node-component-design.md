---
description: 场景节点与组件设计说明，开发新 Node/组件及检查命名与约定
globs: "**/scene/**"
---

# 场景 Node 与组件设计说明

本文档用于辅助**开发新 Node / 新组件**，以及**检查现有功能**是否漏挂组件、命名与约定是否一致。场景节点与组件相关代码位于 **slg-scene** 模块。

---

## 一、概述

### 1.1 节点与组件的关系

- **SceneNode**：场景中所有可见对象的抽象基类（城市、军队、资源点等），负责身份（id、owner）、组件容器、视野/网格、协议转换等。
- **组件（Component）**：以组合方式扩展节点的**单一能力**（如行军、阻挡、交互、驻防、销毁等）。节点通过「挂载」组件获得对应能力，外部通过 `node.getComponent(ComponentEnum.XXX)` 判断是否具备该能力并调用其业务方法。

### 1.2 设计原则

- **功能单一**：一个组件只负责一类能力；类似能力优先复用现有组件或子类扩展。
- **能力按需挂载**：只有需要该能力的节点类型才在 `initComponents()` 中注册对应组件。
- **状态封装**：组件内部状态由组件自身维护，不对外暴露可变引用；外部仅通过组件提供的业务方法交互。

---

## 二、节点体系

### 2.1 类层次与职责

| 类型 | 说明 | 典型实现 |
|------|------|----------|
| **SceneNode&lt;T&gt;** | 基类：id、owner、组件容器、spawned、seeMeList；抽象方法：initComponents、toVO、enterGrid、exitGrid、belongHighLayer、inRange | — |
| **RouteNode&lt;T&gt;** | 行军线：起点/终点（FPosition），线段占格；toVO 为 RouteNodeVO，toArmyVO 委托 ArmyDetail 组件 | PlayerArmy、AssembleArmy |
| **StaticNode&lt;T&gt;** | 静态节点：左下角 Position + 长宽，矩形占格；中心点 centerPosition | PlayerCity |

- **包路径**：`slg-scene/.../scene/node/node/model/`（基类）、`.../model/impl/`（具体节点）。

### 2.2 节点生命周期（简要）

1. **创建**：构造节点 → 调用 **initComponents()** 注册组件（**必须由创建方在节点加入场景前调用**）。
2. **进入场景**：enterGrid → spawned = true。
3. **视野**：玩家进入视野时加入节点的 seeMeList。
4. **离开场景**：exitGrid，清理 seeMeList。

### 2.3 军队相关 Node 与场景存在形式

- **军队相关 node（如 PlayerArmy、AssembleArmy）只能以行军线（RouteNode）的形式出现在场景上**。即：在场景中「可见」时，一定是处于行军状态（有起点、终点，占线段格）。
- **到达目的地后，应从场景中移除**：调用 exitGrid、spawned = false，不再参与 AOI、不再对玩家可见。到达即 despawn，是约定行为。
- **从场景中移除 ≠ 正式销毁**：节点对象可能仍然存在，并处于以下状态之一：
  - **被其他 node 持有**：例如作为驻军（GarrisonComponent）挂在某 StaticNode 上；或作为集结军队的组成成员（MultiArmyDetailComponent 中的 member）被 AssembleArmy 持有。
  - **在战斗队列中**：等待被战斗逻辑执行，尚未真正销毁。
- 因此：**spawn/despawn 只表示「是否在场景上可见、占格」**；节点的创建与真正销毁（DestroyComponent.onDestroy、从业务容器中移除等）由业务在合适的时机在业务线程中处理，且需避免「已 despawn 的 node 仍被场景或其它线程当存活节点使用」的并发与一致性问题。

### 2.4 节点抽象方法

子类必须实现：

- `initComponents()`：在本方法内 `registerComponent(new XxxComponent(this))` 挂载所需组件。
- `toVO()`：转为客户端协议对象（SceneNodeVO 及其子类型）。
- `enterGrid(GridContainer)` / `exitGrid(GridContainer)`：加入/离开 AOI 网格。
- `belongHighLayer()`：节点所属最高视野层级（DETAIL / LAYER1 / LAYER3 等）。
- `inRange(int x1, int y1, int x2, int y2)`：是否在指定矩形视野内。

---

## 三、执行线程与并发约束

**必须严格检查业务的执行线程**，否则易出现竞态、死锁或数据不一致。

### 3.1 场景线程

- **Node 的 spawn 与 despawn（enterGrid / exitGrid、spawned 的写入）只能在场景线程中执行。**
- 场景线程负责 AOI、网格、视野、移动 tick 等与「场景状态」相关的轻量逻辑。
- **场景线程中不可有重度业务**（如复杂战斗结算、大量 DB/IO、长时间计算）；重度业务应投递到以业务 key（如目标 node、玩家 id）的**业务线程**中执行。

### 3.2 业务线程与交互

- **与目标 node 的交互（如行军到达后对目标执行攻击、采集、回城等）必须在以目标为 key 的业务线程中执行。**
- 例如：到达某 StaticNode 后调用 `InteractiveComponent.onInteractedBy(...)` 的逻辑，应在「以该目标 node（或其 owner）为 key」的线程中执行，保证与该目标上的其它操作串行，避免并发写同一目标。

### 3.3 并发与数据安全

- 关注**所使用数据是否有并发风险**：若某对象会被场景线程与业务线程（或多个业务线程）同时访问，需通过锁、线程封闭、或明确「只在单线程读写」的约定来保证安全。
- 典型注意点：node 的 spawned、组件内部状态、被多处持有的集合（如 Garrison 列表、集结成员列表）在跨线程访问时的可见性与修改顺序。

---

## 四、组件体系

### 4.1 核心类与位置

| 类/枚举 | 路径 | 说明 |
|---------|------|------|
| **AbstractNodeComponent&lt;T&gt;** | `scene/node/component/AbstractNodeComponent.java` | 组件基类，T 为可挂载的节点类型；子类实现 getComponentEnum()。 |
| **ComponentEnum** | `scene/node/component/ComponentEnum.java` | 组件类型枚举；新增组件时需在此增加枚举值。 |
| **ComponentContainer** | `scene/node/component/ComponentContainer.java` | 按 ComponentEnum 存储组件；SceneNode 持有并委托 getComponent/registerComponent。 |
| 具体组件 | `scene/node/component/impl/` | Block、SelectTarget、Idle、Interactive 子类、ArmyDetail 子类、Garrison、Destroy 子类等。 |

**实现类目录约定**：组件的**实现类**必须放在 `component/impl/` 目录下；实现类较多时，可按组件的**功能模块**在 `impl` 下划分子包（如 `impl/army`、`impl/interactive` 等）以保持结构清晰。抽象基类若与多种节点共用可放在 `component/`，仅被单一功能使用的基类也可放在 `impl/`。

### 4.2 泛型约定（组件挂载范围）

- **仅限行军线**：`extends AbstractNodeComponent<RouteNode<?>>`，如 SelectTargetComponent、IdleComponent、ArmyDetailComponent。
- **仅限静态节点**：`extends AbstractNodeComponent<StaticNode<?>>`，如 BlockComponent、InteractiveComponent。
- **任意节点**：`extends AbstractNodeComponent<SceneNode<?>>`，如 DestroyComponent。

通过泛型可避免把「仅限 RouteNode」的组件挂到 StaticNode 上（编译期即可发现）。

### 4.3 当前组件枚举与用途（参考）

| 枚举 | 说明 | 适用节点 |
|------|------|----------|
| SelectTarget | 选择目标并朝目标移动（行军） | RouteNode |
| Block | 静态节点占格产生阻挡 | StaticNode |
| Interactive | 可被行军线到达后交互（攻击、采集、回城等） | StaticNode |
| Idle | 行军到达时目标无交互能力时的处理（如到达空地） | RouteNode |
| ArmyDetail | 军队信息与 ArmyVO/FightArmy | RouteNode |
| Garrison | 静态节点上的驻守军队 | StaticNode |
| Destroy | 节点被销毁/解散时的处理 | SceneNode |

### 4.4 组件命名约定

| 层级 | 命名方式 | 示例 |
|------|----------|------|
| 最上层基类 | Abstract + 含义 | AbstractNodeComponent |
| 功能组件（含抽象） | 功能点，不以 Abstract 开头 | DestroyComponent、InteractiveComponent |
| 实现类 | 节点名 + 功能点 | PlayerArmyDismiss、PlayerCityInteractiveComponent |

---

## 五、开发新 Node 的步骤与检查清单

### 5.1 步骤

1. 确定节点是**行军线**还是**静态节点**，继承 `RouteNode<T>` 或 `StaticNode<T>`，放在 `node/model/impl/`。
2. 实现所有抽象方法：`initComponents()`、`toVO()`、`enterGrid`/`exitGrid`、`belongHighLayer()`、`inRange()`。
3. 在 **initComponents()** 中按需调用 `registerComponent(new XxxComponent(this))`，且传入的 `this` 类型满足组件的泛型 T。
4. 确保**创建该节点的所有位置**在节点加入场景前都调用了 **initComponents()**（若当前项目在工厂/创建处统一调用，则保持一致）。

### 5.2 新 Node 检查清单

- [ ] 继承 RouteNode 或 StaticNode，泛型 T 为正确的 NodeOwner 子类。
- [ ] 实现 initComponents()，并注册该节点类型需要的全部组件。
- [ ] 实现 toVO()，返回正确的 VO 子类型。
- [ ] 实现 enterGrid/exitGrid、belongHighLayer、inRange，与节点几何含义一致。
- [ ] 类注释完整（描述、@author、@date）。
- [ ] 节点创建处已调用 initComponents()（若约定在创建方调用）。

---

## 六、开发新组件的步骤与检查清单

### 6.1 步骤

1. 在 **ComponentEnum** 中新增枚举值，如 `Xxx("描述")`。
2. 确定组件**可挂载的节点类型**，选择泛型：`RouteNode<?>` / `StaticNode<?>` / `SceneNode<?>`。
3. 在 **`component/impl/`** 中实现组件类（实现类必须放在 impl 下，可按功能模块划分子包）：
   - 继承 `AbstractNodeComponent<T>`，构造方法接收 `belongNode` 并 `super(belongNode)`。
   - 实现 `getComponentEnum()` 返回对应枚举值。
   - 仅暴露业务方法，内部状态不对外提供 setter 或可变引用；集合类可返回只读视图。
4. 在**需要该能力的节点**的 `initComponents()` 中 `registerComponent(new XxxComponent(this))`。

### 6.2 新组件检查清单

- [ ] 在 ComponentEnum 中已增加对应枚举值。
- [ ] 实现类放在 **component/impl/** 下，按功能模块划分（或划分子包）。
- [ ] 继承 AbstractNodeComponent&lt;T&gt;，T 明确且与使用范围一致（RouteNode/StaticNode/SceneNode）。
- [ ] 实现 getComponentEnum() 并返回本组件枚举常量。
- [ ] 类注释完整（描述、@author、@date）；构造方法 `super(belongNode)`。
- [ ] 功能单一；与现有组件重复时优先复用或子类扩展。
- [ ] 内部变量不对外开放修改；对外仅提供业务方法，集合可返回只读视图。
- [ ] 需要该能力的节点已在 initComponents() 中注册本组件。

### 6.3 使用组件（能力判断）

- 从节点取组件：`node.getComponent(ComponentEnum.XXX)`，非 null 表示具备该能力。
- 调用业务方法前建议先判空，例如：
  - `InteractiveComponent comp = targetNode.getComponent(ComponentEnum.Interactive); if (comp != null) comp.onInteractedBy(arrivingNode, purpose);`
  - `IdleComponent idle = belongNode.getComponent(ComponentEnum.Idle); if (idle != null) idle.onInteractionFailed();`

---

## 七、现有功能检查清单（审查用）

用于排查现有节点是否漏挂组件、组件与枚举是否一致、命名是否符合约定。

### 7.1 节点维度

- [ ] 所有 **RouteNode** 实现类：若需「选择目标并移动」，是否注册了 **SelectTargetComponent**？
- [ ] 所有需「到达后无目标时处理」（如到达空地）的行军线，是否注册了 **IdleComponent**？
- [ ] 所有 **StaticNode** 实现类：若需占格阻挡，是否注册了 **BlockComponent**？若需被行军交互，是否注册了 **Interactive** 子类？
- [ ] 若节点会被销毁/解散（如军队回城解散），是否注册了 **DestroyComponent** 子类？
- [ ] 每个节点类的 **initComponents()** 是否在**节点创建后、加入场景前**被调用（统一在工厂/创建处还是构造内需确认）？

### 7.2 组件维度

- [ ] 每个 **ComponentEnum** 枚举值是否都有对应组件类且 **getComponentEnum()** 与之一致？
- [ ] 组件**实现类**是否均放在 **component/impl/** 下，并按功能模块划分（或划分子包）？
- [ ] 组件类**泛型 T** 是否与注释/实际挂载节点类型一致（如仅挂 RouteNode 的不要写成 SceneNode）？
- [ ] 命名是否符合约定：仅最顶层 AbstractNodeComponent 以 Abstract 开头；功能组件以功能点命名；实现类「节点名+功能点」？
- [ ] 是否有组件**内部状态**被不当暴露（setter 或返回可变集合）？应通过业务方法与只读视图与外部交互。

### 7.3 交互与流程

- [ ] 行军到达目标时，若目标有 **Interactive**，是否统一通过 **InteractiveComponent.onInteractedBy** 进入，并在交互失败时调用行军方的 **IdleComponent.onInteractionFailed()**？
- [ ] 军队回城/解散等「节点销毁」逻辑是否走 **DestroyComponent.onDestroy()**，而不是只写在 Idle 或其他组件里？

### 7.4 军队生命周期与场景

- [ ] 军队相关 node 是否**仅在作为行军线时**才出现在场景上（到达目的地后是否及时 despawn / exitGrid）？
- [ ] 从场景移除后，若 node 被驻军/集结成员/战斗队列持有，是否与「正式销毁」区分清楚，且访问时无并发风险？

### 7.5 执行线程与并发

- [ ] **spawn / despawn（enterGrid、exitGrid、spawned）是否仅在场景线程中执行**？
- [ ] **与目标 node 的交互（如 onInteractedBy）是否投递到以目标为 key 的业务线程中执行**？
- [ ] 场景线程中是否避免了重度业务（复杂计算、DB/IO、长时间阻塞）？
- [ ] 跨线程访问的数据（spawned、组件内部集合、Garrison/集结成员等）是否有明确的线程安全约定或防护？

---

## 八、常见问题与注意点

1. **initComponents 调用时机**：必须在节点**加入场景（enterGrid）之前**完成，否则其他逻辑可能通过 getComponent 拿不到已注册的组件。若项目约定由创建方调用，则所有 new 节点的地方都要保证调用一次。
2. **军队 node 与场景**：军队相关 node 只能以行军线形式出现在场景上，到达目的地后应从场景移除；移除 ≠ 销毁，可能仍被驻军/集结/战斗队列持有（见 **2.3**）。
3. **执行线程**：spawn/despawn 仅限场景线程；与目标交互须在以目标为 key 的业务线程；场景线程不做重度业务；注意跨线程数据并发（见 **三、执行线程与并发约束**）。
4. **同一枚举多实现**：同一 **ComponentEnum** 可有多个实现类（如 ArmyDetail 对应 PlayerArmyDetailComponent 与 MultiArmyDetailComponent），由不同节点在 initComponents 中注册不同实现；容器按枚举只存一个实例。
5. **子类返回同一枚举**：抽象组件（如 DestroyComponent、InteractiveComponent）的子类通常仍返回同一 ComponentEnum（Destroy、Interactive），这样 getComponent(ComponentEnum.Destroy) 拿到的是具体子类实例。
6. **泛型与 registerComponent**：注册时 `registerComponent(new XxxComponent(this))` 中的 `this` 类型必须满足组件的泛型 T，否则编译不通过，可借此避免误挂到不支持的节点类型。

---

## 九、文档维护

- 本文档可根据后续新增节点/组件、架构调整进行增补。
- 组件枚举与职责以代码为准（ComponentEnum、各组件类注释）；本文档仅作归纳与检查辅助。

*最后更新：2026-02-06*
