# 扩展指南

本文档为常见扩展操作提供标准步骤，供开发者和 AI 助手按步骤执行。每个场景标注了可用的 Cursor Skill。

---

## 一、新增协议并接入 Facade

> **相关 Skill**: `protocol-facade-check`、`javadoc`

### 步骤

1. **定义协议类**
   - 客户端协议：`slg-net/src/main/java/com/slg/net/message/clientmessage/模块名/packet/`
   - 内部协议：`slg-net/src/main/java/com/slg/net/message/innermessage/模块名/packet/`
   - 命名：客户端请求 `CM_操作名`，服务端响应 `SM_操作名`，内部消息 `IM_操作名`
   - 协议类需添加类注释和字段注释

2. **在 message.yml 中注册**
   - 路径：`slg-net/src/main/resources/message.yml`
   - 在对应模块分组下添加 `协议号, 类名`
   - 协议号规则：内部消息 100-999，客户端消息 1000+，按模块预留范围分配
   - 确保协议号和类名全局唯一

3. **创建 Facade**
   - 位置：`slg-game/src/main/java/com/slg/game/模块分类/模块名/facade/`
   - 命名：`模块名Facade`
   - 标注 `@Component`
   - 处理方法标注 `@MessageHandler`
   - 方法签名选择：
     - 未绑定阶段（登录/重连/内部协议）：两参 `(NetSession session, 协议类 message)`
     - 已绑定业务协议：三参 `(NetSession session, 协议类 message, Player player)`

4. **校验与补全**
   - 运行 `protocol-facade-check` Skill 校验完整性
   - 运行 `javadoc` Skill 补全注释

### 示例

```java
// 1. 协议类（slg-net/.../clientmessage/hero/packet/）
/**
 * 客户端请求 - 英雄升级
 */
@Getter @Setter
public class CM_HeroLevelUp extends AbstractClientMessage {
    /** 英雄 ID */
    private int heroId;
}

// 2. message.yml
// hero (1300-1399)
// hero:
//   - 1300, CM_HeroLevelUp

// 3. Facade（slg-game/.../develop/hero/facade/）
@Component
public class HeroFacade {
    @MessageHandler
    public void heroLevelUp(NetSession session, CM_HeroLevelUp message, Player player) {
        // 业务逻辑
    }
}
```

---

## 二、新增表配置与 CSV

> **相关 Skill**: `csv-config-check`

### 步骤

1. **编写配置类**
   - 位置：业务模块的 `table` 包下
   - 命名：`*Table`（如 `HeroTable`）
   - 使用 `@Table` 注解声明对应 CSV 文件名
   - 使用 `@TableId` 标注主键字段
   - 使用 `@TableIndex` 标注索引字段（可选）
   - 使用 `@TableRefCheck` 标注外键引用（可选）
   - 添加类注释和字段注释

2. **创建 CSV 文件**
   - 位置：项目根目录 `table/` 下
   - 格式：Luban 格式（第 1 行字段名，第 4 行起数据行）
   - 字段名与配置类字段名对应

3. **注入使用**
   - 在需要使用的类中用 `@Table` 注入：`@Table private List<HeroTable> heroTables;`

4. **校验**
   - 运行 `csv-config-check` Skill 校验格式、关联表

### 示例

```java
/**
 * 英雄配置表
 */
@Getter @Setter
public class HeroTable {
    /** 英雄 ID */
    @TableId
    private int id;
    /** 英雄名称 */
    private String name;
    /** 初始星级 */
    private int star;
    /** 所需升级道具 ID */
    @TableRefCheck(table = ItemTable.class, field = "id")
    private int upgradeItemId;
}
```

---

## 三、新增场景节点组件

> **相关 Skill**: `node-component-dev`

### 步骤

1. **继承 AbstractNodeComponent**
   - 位置：`slg-scene` 的组件包下
   - 用泛型限定适用节点类型（如 `RouteNode`、`StaticNode`）
   - 功能保持单一，一个组件只做一件事

2. **注册到 ComponentEnum**
   - 在 `ComponentEnum` 枚举中添加新组件的枚举值
   - 指定组件的 Class 引用

3. **在节点中获取使用**
   - 通过 `node.getComponent(ComponentEnum.XXX)` 获取组件实例

4. **补全注释**
   - 运行 `javadoc` Skill 补全类和方法注释

### 设计原则

- **功能单一**：每个组件只负责一种能力（如阻挡、目标选择）
- **优先复用**：先检查已有组件或子类是否可复用
- **泛型约束**：通过泛型限定组件只能被特定节点类型使用

---

## 四、新增 RPC 接口与实现

> **相关 Skill**: `javadoc`、`rpc-disconnect-check`

### 步骤

1. **定义 RPC 接口**
   - 位置：`slg-net/src/main/java/com/slg/net/rpc/impl/模块名/`
   - 命名：`I模块名RpcService`（如 `ISceneRpcService`）
   - 方法添加 `@RpcMethod` 注解
   - 路由参数标注 `@RpcRouteParams`，线程键标注 `@ThreadKey`

2. **配置 @RpcMethod 属性**
   - `routeClz`：路由策略类（默认 `ServerIdRoute`，场景常用 `PlayerCurrentSceneRoute`）
   - `useModule`：执行模块（默认 `TaskModule.PLAYER`）
   - `timeoutMillis`：超时时间（默认 30000ms）

3. **实现接口**
   - 在业务模块（slg-game 或 slg-scene）中实现接口
   - 注册为 Spring Bean（`@Component` 或 `@Service`）

4. **返回值选择**
   - `void`：单向调用（fire-and-forget）
   - `CompletableFuture<T>`：有返回值，调用方决定同步或异步消费

5. **线程安全**
   - 多链模块中可用 `.join()` 同步等待
   - 单链模块中**禁止** `.join()`，必须用异步回调 + 手动线程分派

6. **校验**
   - 运行 `rpc-disconnect-check` Skill 检查断线安全性
   - 运行 `javadoc` Skill 补全注释

### 示例

```java
// 1. 接口定义（slg-net/.../rpc/impl/scene/）
public interface ISceneRpcService {
    @RpcMethod(routeClz = ServerIdRoute.class, useModule = TaskModule.SCENE)
    CompletableFuture<Integer> enterScene(
        @RpcRouteParams int serverId,
        @ThreadKey long playerId,
        int sceneId
    );
}

// 2. 实现（slg-scene）
@Component
public class SceneRpcServiceImpl implements ISceneRpcService {
    @Override
    public CompletableFuture<Integer> enterScene(int serverId, long playerId, int sceneId) {
        // 场景入场逻辑
        return CompletableFuture.completedFuture(0);
    }
}

// 3. 调用（slg-game，多链模块中可 .join()）
int result = sceneRpcService.enterScene(serverId, playerId, sceneId).join();
```

---

## 五、slg-web 参考代码模式

> 无专用 Skill，按以下流程操作

### 背景

`slg-web` 的参考实现位于**导量服文件夹**（`导量服/icefire-web`），是传统 Spring MVC + WAR 的 Web 应用。遇到与导量服相关的业务疑问时，应优先查阅参考代码。

### 步骤

1. **查阅参考代码**
   - 在 `导量服/icefire-web` 目录中查找对应功能的实现
   - 理解其业务逻辑、数据模型和接口定义

2. **对照 slg-web 现有代码**
   - 检查 slg-web 中是否已有同类实现
   - 确认技术栈差异：
     - 参考代码：javax 命名空间 + 传统 WAR
     - slg-web：jakarta 命名空间 + Spring Boot 3.x

3. **移植与适配**
   - 将参考代码中的业务逻辑移植到 slg-web
   - 适配 jakarta 命名空间（`javax.servlet` → `jakarta.servlet` 等）
   - 使用项目的 MySQL 框架（`@EnableMysql` + `BaseMysqlEntity` + `EntityCache`）
   - 使用项目的安全框架（Shiro 2.0 jakarta）

4. **注意事项**
   - 不要直接复制粘贴，需理解后适配项目规范
   - 数据库操作使用 `EntityCache`，禁止使用 `JpaRepository`
   - 添加 JavaDoc 注释

---

*文档版本：1.0 | 创建日期：2026-03-06*
