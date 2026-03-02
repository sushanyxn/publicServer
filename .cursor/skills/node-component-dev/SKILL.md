---
name: node-component-dev
description: 基于 AbstractNodeComponent 开发场景节点组件。指导功能单一化、复用或子类已有组件、通过组件枚举 ComponentEnum 注册与获取组件、用泛型限定组件适用节点类型（如 RouteNode/StaticNode）。在用户要求新增/开发节点组件、为 node 增加能力或实现类似 BlockComponent/SelectTargetComponent 的功能时使用。
---

# 节点组件开发

## 目标与原则

1. **基类**：所有组件继承 `AbstractNodeComponent<T>`，`T` 为可挂载的节点类型（如 `RouteNode<?>`、`StaticNode<?>`）。
2. **组件枚举**：每种组件对应 `ComponentEnum` 中的一个枚举值；容器按枚举注册与获取，组件必须实现 `getComponentEnum()`。
3. **功能单一**：一个组件只负责一类能力；若有类似能力，优先**复用**现有组件或在已有组件上**子类扩展**，避免重复实现。
4. **能力判断**：通过 `node.getComponent(ComponentEnum.XXX)` 判断该 node 是否具备该能力；取到非 null 即表示具备。
5. **使用范围**：通过**泛型**声明限定或缩小组件的使用范围（例如行军/选目标组件仅限 `RouteNode`，阻挡组件仅限 `StaticNode`）。
6. **状态封装**：若无特别要求，**不要开放对组件内部变量的修改**；变量由组件自身维护，外部不参与维护，只能调用组件提供的**业务方法**（如 `addMember`、`toPlayerArmyVO` 等）。

## 代码位置与结构

- **组件枚举**：`slg-scene/.../scene/node/component/ComponentEnum.java`，新增组件时需在此增加对应枚举值。
- **组件基类**：`slg-scene/.../scene/node/component/AbstractNodeComponent.java`，子类必须实现 `getComponentEnum()`。
- **组件容器**：`ComponentContainer`，由 `SceneNode.componentContainer` 持有；按枚举存取：`getComponent(ComponentEnum)`、`registerComponent(component)`（内部用 `component.getComponentEnum()` 作为 key）。
- **组件实现类**：**必须**放在 `slg-scene/.../scene/node/component/impl/` 目录下，按组件的**功能模块**划分；实现类较多时可在 `impl` 下划分子包（如 `impl/army`、`impl/interactive`）以保持结构清晰。与现有 `BlockComponent`、`SelectTargetComponent`、`PlayerArmyIdle`、`AssembleDismissComponent` 等同级或归入对应功能子包。
- **节点挂载**：在具体 SceneNode 子类的 `initComponents()` 中调用 `registerComponent(new XxxComponent(this))` 注册组件。

## 命名约定

- **最上层基类**：仅最顶层的组件基类以 `Abstract` 开头（如 `AbstractNodeComponent`）。
- **功能组件（中间层）**：其下的功能组件**即使是抽象类**也以**具体功能点**命名，不以 Abstract 开头。例如：抽象销毁能力用 `DestroyComponent`，抽象交互能力用 `InteractiveComponent`。
- **实现类**：再往下的具体实现类命名为 **「节点名（声明的泛型 node）+ 功能点」**。例如：挂载在 `PlayerArmy` 上的解散实现 = `PlayerArmy` + `Dismiss` → `PlayerArmyDismiss`；挂载在 `PlayerCity` 上的交互实现 = `PlayerCity` + `Interactive` → `PlayerCityInteractiveComponent`。

| 层级       | 命名方式           | 示例                          |
|------------|--------------------|-------------------------------|
| 最上层基类 | Abstract + 含义    | AbstractNodeComponent        |
| 功能组件   | 功能点（可抽象）   | DestroyComponent, InteractiveComponent |
| 实现类     | 节点名 + 功能点    | PlayerArmyDismiss, PlayerCityInteractiveComponent |

## 定义组件

### 泛型约定

- `AbstractNodeComponent<T extends SceneNode<?>>`：`T` 表示该组件只能挂载在哪种节点上。
- **仅限行军线**：`extends AbstractNodeComponent<RouteNode<?>>`（如 `SelectTargetComponent`）。
- **仅限静态节点**：`extends AbstractNodeComponent<StaticNode<?>>`（如 `BlockComponent`）。
- **任意节点**：`extends AbstractNodeComponent<SceneNode<?>>` 或更具体的共同基类（若存在）。

### 组件枚举

- **新增组件时**：在 `ComponentEnum` 中增加一个枚举值，如 `Xxx("描述")`，构造器已统一为 `ComponentEnum(String desc) { this.desc = desc; }`。
- 每个组件类必须实现基类抽象方法：`@Override public ComponentEnum getComponentEnum() { return ComponentEnum.Xxx; }`。

### 类与构造

- 组件类需有**类注释**（简要描述、`@author`、`@date`），符合项目规范。
- 构造方法接收 `belongNode`（类型为泛型 `T`），并调用 `super(belongNode)`。
- 实现 `getComponentEnum()` 返回本组件对应的枚举值。
- 通过 `getBelongNode()` 访问所属节点（基类已提供）。

示例（限定 StaticNode）：

```java
/**
 * 阻挡组件。静态 node 出生在 scene 中时产生阻挡点。
 * @author xxx
 * @date yyyy-MM-dd
 */
public class BlockComponent<T extends StaticNode<?>> extends AbstractNodeComponent<T> {
    public BlockComponent(T belongNode) {
        super(belongNode);
    }
    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.Block;
    }
}
```

示例（限定 RouteNode）：

```java
/**
 * 选择目标组件。表示 node 具有选择目标并朝目标移动的能力。
 * @author xxx
 * @date yyyy-MM-dd
 */
public class SelectTargetComponent extends AbstractNodeComponent<RouteNode<?>> {
    public SelectTargetComponent(RouteNode<?> belongNode) {
        super(belongNode);
    }
    @Override
    public ComponentEnum getComponentEnum() {
        return ComponentEnum.SelectTarget;
    }
}
```

## 使用组件（能力判断）

- 从**节点**取组件：`node.getComponent(ComponentEnum.XXX)`，若不为 null 表示该 node 具备该能力。
- 从**组件容器**取：`node.getComponentContainer().getComponent(ComponentEnum.XXX)`，等价于上者（SceneNode 已委托给 componentContainer）。

示例：

```java
SelectTargetComponent comp = node.getComponent(ComponentEnum.SelectTarget);
if (comp != null) {
    // 该 node 具有选目标/行军能力
}
InteractiveComponent interactable = targetNode.getComponent(ComponentEnum.Interactive);
if (interactable != null) {
    interactable.onInteractedBy(belongNode);
}
```

## 状态与封装

- **不开放内部变量**：若无特别要求，组件内部变量（如列表、状态字段）不对外提供 setter 或返回可变引用；变量由组件**自身维护**，外部**不参与维护**。
- **只暴露业务方法**：外部仅通过组件提供的**业务方法**与组件交互（如 `addMember(PlayerArmy)`、`removeMember(PlayerArmy)`、`toPlayerArmyVO()`）；需要“增删改”时在组件内实现对应方法，由组件在方法内修改自身变量。
- **只读视图**：若需对外展示集合内容，可返回**只读视图**（如 `Collections.unmodifiableList(...)`），避免调用方直接修改组件内部集合。

## 复用与扩展

- **已有类似功能**：先查看 `component/impl/` 与 `ComponentEnum` 是否已有对应组件（如 SelectTarget、Block、Interactive、Idle）。能直接复用则用 `getComponent(ComponentEnum.xxx)` 使用；若需变体，优先**继承该组件**做子类（子类返回同一枚举或需在枚举中新增值，视设计而定），而不是新建一个功能重叠的组件。
- **功能单一**：一个组件只做一类事；若新需求可归入现有组件的职责，在现有组件上扩展方法或子类，避免“大而全”的新组件。

## 注册组件

- 在**具体节点子类**的 `initComponents()` 中注册：`registerComponent(new XxxComponent(this))`。
- 仅当该节点类型确实需要该能力时才注册；例如只有行军线实现类注册 `SelectTargetComponent`，只有静态节点实现类注册 `BlockComponent`。
- 注册时传入的 `this` 类型必须满足组件的泛型 `T`（如 RouteNode 子类传 this 给 `SelectTargetComponent`），否则编译不通过。

## 检查清单

开发或审查组件时确认：

- [ ] **命名**：最上层仅 `AbstractNodeComponent` 以 Abstract 开头；功能组件以功能点命名（抽象类也不加 Abstract）；实现类以「节点名 + 功能点」命名（如 PlayerArmyDismiss）。
- [ ] **位置**：组件的实现类放在 **component/impl/** 下，按功能模块划分（或划分子包）。
- [ ] 在 `ComponentEnum` 中已增加本组件的枚举值（新增组件时）。
- [ ] 继承 `AbstractNodeComponent<T>` 且 `T` 明确（RouteNode/StaticNode/SceneNode 等）。
- [ ] 实现 `getComponentEnum()` 并返回本组件对应的枚举常量。
- [ ] 类注释完整（描述、@author、@date）。
- [ ] 构造方法接收 `belongNode` 并 `super(belongNode)`。
- [ ] 功能单一；与现有组件重复时优先复用或子类扩展。
- [ ] 内部变量不对外开放修改；外部仅通过组件提供的业务方法交互，集合类可返回只读视图。
- [ ] 使用处通过 `node.getComponent(ComponentEnum.XXX)` 判断是否具备能力。
- [ ] 需挂载该能力的节点在 `initComponents()` 中 `registerComponent(new 组件(this))`。
