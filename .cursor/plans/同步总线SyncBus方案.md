# SyncBus - 跨进程实体同步总线方案（v3）

## 一、核心思路

在 `slg-net/syncbus` 包中构建一套轻量级的注解驱动同步辅助系统：

- 定义 **`SyncModule` 枚举**（显式 ID）作为同步模块标识
- Holder 和 Cache 通过 **`@SyncEntity`** 类注解声明所属模块，无需实例化即可完成扫描
- 用 **`@SyncField`** 标记需要同步的字段，两端字段名需一一对应（由开发者保证，不做跨进程配对校验）
- 业务代码只需一行 `SyncBus.sync(holder, fieldName)` 即可完成同步
- 底层通过 **RPC** 通信（`ISyncBusRpcService`），复用现有 RPC 框架：
  - **远程场景**（Game -> Scene）：自动走 WebSocket 网络通道
  - **本地场景**（同一进程内）：CglibProxy 检测 `isLocal()` 后直接本地调用，零网络开销

### 架构约束

1. **辅助系统定位**：SyncBus 是轻量辅助工具，职责仅为"把指定字段的值传送到对端"，不保证强一致性
2. **字段对齐由开发者保证**：两端的 `@SyncField` 字段名和序列化格式需开发者自行保持一致。字段不匹配时不会阻止启动，而是在运行时产生 error 日志便于排查
3. **一对一绑定**：每个 ISyncHolder / ISyncCache 实现类只能绑定一个 SyncModule，每个 SyncModule 在同一进程内只有一个 Holder 类和一个 Cache 类。如需将同一数据同步到不同类型的 Cache，应新建独立的 Holder 子类或 VO 作为独立 SyncModule
4. **线程安全由业务保证**：`SyncBus.sync()` 不做并发控制，调用方须确保在对应实体的执行器链内调用（通常自然满足，因为业务修改字段本身就在 Player 执行器链中）
5. **fire-and-forget**：同步为单向发送，不等待响应。当前版本不对异常行为（网络故障、对端未就绪等）造成的数据丢失做出反应，业务可在需要保证数据完整性的节点主动触发 `syncAll` 全量同步进行补偿，具体时机由业务自行判断（如实体创建、断线重连、跨场景迁移等）

## 二、业务使用示例

```java
// ============================================================
// 场景1：简单字段，无需自定义编解码（大多数情况）
// ============================================================

// === Holder 端（slg-game PlayerEntity）===
@SyncEntity(SyncModule.PLAYER)
public class PlayerEntity extends BaseEntity<Long> implements ISyncHolder {
    @SyncField                          // 默认限流：1秒内最多同步一次
    private long allianceId;
    @SyncField
    private String playerName;
    @SyncField(syncInterval = 0)        // 不限流，每次调用立即发送
    private int vipLevel;
    @SyncField(syncInterval = 5)        // 5秒内最多同步一次
    private long power;
    // 非同步字段无需标注
    private HeroPlayerInfo heroPlayerInfo;

    @Override
    public long getSyncId() { return id; }
    @Override
    public int[] syncTargetServerIds() { return new int[]{ sceneServerId }; }
}

// === Cache 端（slg-scene ScenePlayerEntity）===
@SyncEntity(SyncModule.PLAYER)
public class ScenePlayerEntity extends BaseEntity<Long> implements ISyncCache {
    @SyncField
    private long allianceId;
    @SyncField
    private String playerName;

    @Override
    public long getSyncId() { return id; }
}

// ============================================================
// 场景2：自定义编解码 —— 两端 model 不同，编码器和解码器分别定义
// 例：Game 侧 HeroInfo(复杂对象) -> Scene 侧 SceneHeroSnapshot(精简对象)
// ============================================================

// --- Holder 端编码器（定义在 slg-game，面向 Game 的 model）---
public class HeroInfoEncoder implements ISyncFieldEncoder<HeroInfo> {
    @Override
    public String encode(HeroInfo value) {
        // 只提取 Scene 侧需要的字段，序列化为 JSON
        Map<String, Object> map = new HashMap<>();
        map.put("heroId", value.getHeroId());
        map.put("level", value.getLevel());
        map.put("power", value.calcPower());
        return JsonUtil.toJson(map);
    }
}

// --- Cache 端解码器（定义在 slg-scene，面向 Scene 的 model）---
public class SceneHeroSnapshotDecoder implements ISyncFieldDecoder<SceneHeroSnapshot> {
    @Override
    public SceneHeroSnapshot decode(String data) {
        return JsonUtil.fromJson(data, SceneHeroSnapshot.class);
    }
}

// Holder 端使用 encoder
@SyncEntity(SyncModule.PLAYER)
public class PlayerEntity extends BaseEntity<Long> implements ISyncHolder {
    @SyncField(encoder = HeroInfoEncoder.class)
    private HeroInfo heroInfo;       // Game 侧的完整模型
    // ...
}

// Cache 端使用 decoder
@SyncEntity(SyncModule.PLAYER)
public class ScenePlayerEntity extends BaseEntity<Long> implements ISyncCache {
    @SyncField(decoder = SceneHeroSnapshotDecoder.class)
    private SceneHeroSnapshot heroInfo;  // Scene 侧的精简模型（字段名必须相同）
    // ...
}

// ============================================================
// Cache 查找器 & 业务调用（与场景无关，始终相同）
// ============================================================

// === Cache 查找器（slg-scene，每种 Cache 实体实现一次）===
@Component
public class PlayerSyncCacheResolver implements ISyncCacheResolver<ScenePlayerEntity> {
    @Override
    public SyncModule getSyncModule() { return SyncModule.PLAYER; }
    @Override
    public ScenePlayerEntity resolve(long syncId) {
        return SpringContext.getScenePlayerManager().getScenePlayerEntity(syncId);
    }
}

// === 业务调用（每次字段变更只需这样）===
playerEntity.setAllianceId(newAllianceId);
playerEntity.saveField(PlayerEntity.Fields.allianceId);             // 持久化
SyncBus.sync(playerEntity, PlayerEntity.Fields.allianceId);         // 同步到远端
// 或批量同步所有 @SyncField 字段:
SyncBus.syncAll(playerEntity);

// === 清理限流状态（实体生命周期结束时调用）===
// 例如：玩家下线时
SyncBus.remove(playerEntity.getSyncId());
```

## 三、数据流

```mermaid
sequenceDiagram
    participant Biz as 业务代码
    participant SB as SyncBus
    participant TH as 限流状态
    participant SCH as GlobalScheduler
    participant Enc as ISyncFieldEncoder
    participant Proxy as CglibProxy
    participant RPC as ISyncBusRpcService
    participant Impl as SyncBusRpcService
    participant Dec as ISyncFieldDecoder
    participant Cache as ScenePlayerEntity

    Biz->>SB: sync(playerEntity, "heroInfo")

    alt syncInterval == 0（不限流）
        SB->>SB: 直接进入发送流程
    else syncInterval > 0（限流）
        SB->>TH: 检查限流状态
        alt 距上次发送 >= syncInterval
            TH-->>SB: 允许发送，更新 lastSyncTime
        else 距上次发送 < syncInterval 且无 pending
            TH-->>SB: 拦截，标记 pending=true
            SB->>SCH: schedule 延迟发送（剩余时间后触发）
            Note over SB: 本次调用结束，不发送
        else 距上次发送 < syncInterval 且已有 pending
            TH-->>SB: 拦截，什么都不做
            Note over SB: 定时器到期后会读取最新值
        end
    end

    SB->>SB: MethodHandle 读取字段值
    alt 有自定义 Encoder
        SB->>Enc: encoder.encode(heroInfo) → String
    else 无自定义 Encoder
        SB->>SB: JsonUtil.toJson(value) → String
    end
    Note over SB: 所有字段统一编码为 String，组装 Map&lt;String, String&gt;
    SB->>SB: holder.syncTargetServerIds()
    loop 每个 targetServerId
        SB->>Proxy: rpcService.receiveSyncData(serverId, entityId, moduleId, json)
        alt isLocal=true
            Proxy->>Impl: 直接调用
        else 远程调用
            Proxy->>RPC: WebSocket IM_RpcRequest
            RPC-->>Impl: 远端接收
        end
        Impl->>Impl: ISyncCacheResolver.resolve(entityId)
        Note over Impl: 反序列化 fieldData 为 Map&lt;String, String&gt;，逐字段处理
        alt 有自定义 Decoder
            Impl->>Dec: decoder.decode(stringValue)
        else 无自定义 Decoder
            Impl->>Impl: JsonUtil.fromJson(stringValue, fieldType)
        end
        Impl->>Cache: MethodHandle set 字段值
        Impl->>Cache: onSyncUpdated 回调
    end
```

## 四、SyncModule 枚举设计

位于 `com.slg.net.syncbus.SyncModule`：

```java
public enum SyncModule {
    /** 玩家同步：PlayerEntity(Game) -> ScenePlayerEntity(Scene) */
    PLAYER(1),
    // 未来扩展：ALLIANCE(2), GUILD(3), ...
    ;

    private final int id;

    SyncModule(int id) { this.id = id; }

    public int getId() { return id; }

    /** id -> 枚举的快速查找表 */
    private static final Map<Integer, SyncModule> ID_MAP = new HashMap<>();
    static {
        for (SyncModule m : values()) {
            if (ID_MAP.put(m.id, m) != null) {
                throw new IllegalStateException("SyncModule id 重复: " + m.id);
            }
        }
    }

    public static SyncModule fromId(int id) {
        return ID_MAP.get(id);
    }
}
```

- 枚举使用**显式 ID**（而非 ordinal），确保新增枚举值不会影响已有协议
- 枚举纯粹作为标识，不持有类引用（避免跨模块编译依赖）
- 启动时 SyncBusRegistry 通过 ClassGraph 扫描 `@SyncEntity` 注解，按枚举值归类（无需实例化）

## 五、RPC 接口设计

位于 `com.slg.net.rpc.impl.syncbus.ISyncBusRpcService`：

```java
public interface ISyncBusRpcService {
    @RpcMethod(routeClz = ServerIdRoute.class, useModule = TaskModule.PLAYER)
    void receiveSyncData(@RpcRouteParams int targetServerId,
                         @ThreadKey long entityId,
                         int syncModuleId,
                         String fieldData);
}
```

- `targetServerId` + `@RpcRouteParams`：按目标服务器 ID 路由，本地则直接调用
- `entityId` + `@ThreadKey`：保证同一实体的同步操作在 PLAYER 多链执行器中串行执行
- `syncModuleId`：`SyncModule.getId()`，接收端通过 `SyncModule.fromId()` 还原为枚举
- `fieldData`：JSON 格式的 `Map<String, String>` 字符串。所有字段值统一编码为 String（无论是自定义 encoder 还是默认 `JsonUtil.toJson()`），避免类型混杂导致的双重编码问题。示例：`{"allianceId":"123","playerName":"\"test\"","heroInfo":"{\"heroId\":1}"}`
- 返回 `void`：同步为单向 fire-and-forget，无需等待响应

**为什么用 RPC 而非自定义 IM 消息**：

1. 自动处理本地/远程：CglibProxy 通过 `isLocal()` 判断，本地直接调用，远程走网络
2. 自动线程分派：`useModule + @ThreadKey` 自动分派到正确的执行器链
3. 无需新增 IM 协议号、MessageHandler，减少框架侵入

## 六、核心类设计

框架代码位于 `com.slg.net.syncbus` 及其子包下（见"九、文件清单"中的包结构总览）：

### 6.1 枚举、注解与编解码器

- **`SyncModule`** - 同步模块枚举，使用显式 ID 标识不同的同步实体对
- **`@SyncEntity`** - 类级别注解，声明该类所属的 SyncModule。扫描时直接读注解，无需实例化
  - `SyncModule value()` - 必填，所属同步模块
- **`@SyncField`** - 标注在字段上，表示该字段参与同步，两端字段名需相同
  - 可选属性 `encoder`：指定编码器类，**仅在 Holder 端生效**，如 `@SyncField(encoder = HeroInfoEncoder.class)`
  - 可选属性 `decoder`：指定解码器类，**仅在 Cache 端生效**，如 `@SyncField(decoder = SceneHeroSnapshotDecoder.class)`
  - 两个属性可独立使用：Holder 指定 encoder 而 Cache 指定 decoder，各自面向本模块的 model
  - 均未指定时使用默认 Jackson ObjectMapper 序列化/反序列化
  - 可选属性 `syncInterval`：同步限流间隔（秒），**仅在 Holder 端生效**，默认值 `1`
    - `syncInterval = 0`：不限流，每次 `sync()` 调用立即发送
    - `syncInterval = N`（N > 0）：N 秒内该字段最多实际发送一次。限流期间的多次调用会被合并，定时器到期后自动读取最新值发送
    - 详见"限流机制"章节
- **`ISyncFieldEncoder<T>`** - 自定义字段编码器接口（Holder 端使用）
  - `String encode(T value)` - 将 Holder 的字段值编码为字符串
  - 实现类定义在 Holder 所在模块（如 slg-game），面向 Holder 端的 model 工作
  - 必须有无参构造器，框架启动时自动实例化并缓存
- **`ISyncFieldDecoder<T>`** - 自定义字段解码器接口（Cache 端使用）
  - `T decode(String data)` - 将字符串解码为 Cache 端的字段值
  - 实现类定义在 Cache 所在模块（如 slg-scene），面向 Cache 端的 model 工作
  - 必须有无参构造器，框架启动时自动实例化并缓存

### 6.2 接口

- **`ISyncHolder`** - 增强现有接口（Holder 端实体实现）
  - `long getSyncId()` - 实体唯一标识
  - `int[] syncTargetServerIds()` - 已有方法，返回同步目标服务器 ID 列表
- **`ISyncCache`** - 增强现有接口（Cache 端实体实现）
  - `long getSyncId()` - 实体唯一标识
  - `default void onSyncUpdated(String fieldName, Object newValue) {}` - 可选回调，字段被同步更新后触发
- **`ISyncCacheResolver<T extends ISyncCache>`** - 业务模块实现，每种 Cache 实体一个
  - `SyncModule getSyncModule()` - 对应的同步模块
  - `T resolve(long syncId)` - 按 ID 查找 Cache 实体

> **注意**：ISyncHolder 和 ISyncCache 不再包含 `getSyncModule()` 方法，模块归属通过类上的 `@SyncEntity` 注解声明。

### 6.3 元数据

由于 Game 和 Scene 是独立进程（classpath 不同），元数据按角色分为两套，每端只构建自己能看到的部分：

- **`SyncHolderFieldMeta`** - Holder 端单个同步字段的元数据
  - `fieldName` - 字段名（与 Cache 端约定一致）
  - `fieldType` - Holder 端字段类型
  - `getter` (MethodHandle) - 从 Holder 读取字段值
  - `encoder` (ISyncFieldEncoder, 可为 null) - 自定义编码器
  - `syncIntervalMs` (long) - 同步限流间隔（毫秒），从 `@SyncField.syncInterval` 转换，0 表示不限流
  - **编码流程**：encoder != null ? `encoder.encode(value)` : `JsonUtil.toJson(value)`（结果统一为 `String`）
- **`SyncHolderMeta`** - Holder 端实体元数据
  - `syncModule`, `holderClass`, `Map<String, SyncHolderFieldMeta> fields`
- **`SyncCacheFieldMeta`** - Cache 端单个同步字段的元数据
  - `fieldName` - 字段名
  - `fieldType` - Cache 端字段类型
  - `setter` (MethodHandle) - 向 Cache 写入字段值
  - `decoder` (ISyncFieldDecoder, 可为 null) - 自定义解码器
  - **解码流程**：接收到的 value 始终为 `String`。decoder != null ? `decoder.decode(stringValue)` : `JsonUtil.fromJson(stringValue, fieldType)`
- **`SyncCacheMeta`** - Cache 端实体元数据
  - `syncModule`, `cacheClass`, `Map<String, SyncCacheFieldMeta> fields`, `ISyncCacheResolver<?> resolver`

### 6.4 核心组件

- **`SyncBusRegistry`** (@Component, InitializingBean) - 启动时扫描与元数据构建
  - 使用 ClassGraph 扫描 `com.slg` 包下所有标注了 `@SyncEntity` 的类
  - 通过注解值获取 SyncModule，**无需临时实例化**
  - 根据类实现的接口区分角色：实现 ISyncHolder 的归为 Holder，实现 ISyncCache 的归为 Cache
  - **Holder 端元数据构建**：扫描 `@SyncField` 字段，预编译 getter MethodHandle，实例化 encoder
  - **Cache 端元数据构建**：扫描 `@SyncField` 字段，预编译 setter MethodHandle，实例化 decoder
  - 收集所有 ISyncCacheResolver Bean，建立 SyncModule -> Resolver 映射
  - 同一进程可能只有 Holder 或只有 Cache（正常情况），不做跨角色配对校验
- **`SyncBus`** (@Component，提供静态方法) - 业务入口（Holder 端使用）
  - `static void sync(ISyncHolder holder, String... fieldNames)` - 同步指定字段（受限流控制）
  - `static void syncAll(ISyncHolder holder)` - 同步所有 @SyncField 字段（**与 sync 完全独立，直接读取所有字段立即全量发送**，不影响 sync 的限流状态）
  - `static void remove(long syncId)` - 移除指定实体的所有限流状态。限流状态在定时发送后会自动清理，此方法作为安全兜底。业务方可在实体生命周期结束时调用（如玩家下线、实体销毁）。**必须在对应实体的执行器链内调用**
  - 内部持有限流状态 `ConcurrentHashMap<Long/*syncId*/, Map<String/*fieldName*/, SyncThrottleState>>`，以实体 ID 为 key。限流状态采用定时器驱动自清理机制，无新更新时自动移除
  - **sync() 内部流程**：
    1. 通过 `@SyncEntity` 注解获取 SyncModule，查找 `SyncHolderMeta`
    2. 对每个 fieldName，检查 `SyncHolderFieldMeta.syncIntervalMs`：
       - `== 0`：不限流，直接进入发送流程
       - `> 0`：走限流判断（详见"限流机制"章节）
    3. 对允许发送的字段：通过 MethodHandle 读取值，编码为 String（自定义 encoder 或 `JsonUtil.toJson()`）
    4. 构建 fieldData：`Map<String, String>`（字段名 → 编码后的 String），整体序列化为 JSON 字符串
    5. 遍历 `holder.syncTargetServerIds()`，对每个 serverId 调用 `ISyncBusRpcService.receiveSyncData()`
  - 持有 `@RpcRef ISyncBusRpcService` 代理，CglibProxy 自动处理本地/远程
- **`SyncBusRpcService`** (@Component, 实现 ISyncBusRpcService) - 接收端（Cache 端使用）
  - `receiveSyncData()` 方法由 RPC 框架自动分派到 PLAYER 链（entityId 为 key）
  - 根据 `syncModuleId` 通过 `SyncModule.fromId()` 还原枚举，查找 `SyncCacheMeta`
  - 通过 `ISyncCacheResolver.resolve(entityId)` 获取 Cache 实体
  - 将 JSON fieldData 反序列化为 `Map<String, String>`，逐字段处理：
    - 查找对应的 `SyncCacheFieldMeta`，找不到则打印 error 日志并跳过（字段不匹配）
    - 解码：value 为 String 类型。decoder != null ? `decoder.decode(value)` : `JsonUtil.fromJson(value, fieldType)`
    - 解码失败打印 error 日志并跳过（类型不匹配），不影响其他字段
    - 通过 MethodHandle 写入字段值
    - 调用 `cache.onSyncUpdated(fieldName, value)` 回调
  - Cache 实体不存在时打印 debug 日志（对端可能尚未创建）

## 七、启动元数据构建流程

`SyncBusRegistry.afterPropertiesSet()` 执行：

1. **ClassGraph 扫描** `com.slg` 包下所有标注了 `@SyncEntity` 注解的类
   - 读取注解值获取 SyncModule（纯注解读取，无需实例化）
   - 按实现的接口区分：ISyncHolder → Holder 类，ISyncCache → Cache 类
2. **构建 Holder 端元数据**：对每个 Holder 类
   - 收集所有 `@SyncField` 字段
   - 预编译 getter MethodHandle
   - 若 `@SyncField` 指定了 encoder，实例化编码器并缓存
   - 组装 `SyncHolderMeta`
3. **构建 Cache 端元数据**：对每个 Cache 类
   - 收集所有 `@SyncField` 字段
   - 预编译 setter MethodHandle
   - 若 `@SyncField` 指定了 decoder，实例化解码器并缓存
   - 组装 `SyncCacheFieldMeta`
4. **收集 Resolver**：从 Spring 容器获取所有 `ISyncCacheResolver` Bean
   - 按 `getSyncModule()` 建立 SyncModule -> Resolver 映射
   - 将 Resolver 关联到对应的 `SyncCacheMeta`
   - 有 Cache 类但无 Resolver：打印 warn 日志（该进程可能只作为 Holder 端）

> **无跨进程配对校验**：Game 进程只能扫到 Holder 类，Scene 进程只能扫到 Cache 类，两端独立构建各自的元数据。字段名或类型不匹配时在运行时 `receiveSyncData` 中以 error 日志暴露，不阻止启动。

## 八、线程安全

- **Holder 端**：`SyncBus.sync()` 在当前线程执行（通常是 Player executor），通过 MethodHandle 读取字段值，无锁操作。调用方须确保在对应实体的执行器链内调用
- **Cache 端**：RPC 框架根据 `useModule = TaskModule.PLAYER` + `@ThreadKey long entityId` 自动分派到 PLAYER 多链执行器中，保证同一实体串行执行
- **本地调用**：CglibProxy 检测 `inThread(PLAYER, entityId)` 为 true 时同步执行，无额外线程切换

## 九、限流机制

### 9.1 设计目标

对于高频变更的字段（如战力值、坐标等），短时间内可能触发大量 `sync()` 调用。限流机制确保每个字段在 `syncInterval` 秒内最多实际发送一次 RPC，中间的多次修改自动合并，定时器到期后发送最新值。

### 9.2 @SyncField 限流参数

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SyncField {
    // ... encoder/decoder 属性 ...

    /**
     * 同步限流间隔（秒），仅在 Holder 端生效
     * - 0：不限流，每次 sync() 立即发送
     * - N（N > 0）：N 秒内该字段最多实际发送一次
     * 默认 1 秒
     */
    int syncInterval() default 1;
}
```

### 9.3 限流状态

```java
/**
 * 每个实体每个字段的限流状态（SyncBus 内部维护）
 * 同一实体的所有操作在同一执行器链内串行，无需加锁
 *
 * 状态存在即表示"正在限流周期中"，定时器到期后若无新更新则自动移除
 */
private static class SyncThrottleState {
    /** 是否有新的更新等待发送（定时器到期时检查） */
    boolean dirty;
}

/**
 * 限流状态表：以实体 syncId 为 key
 * 限流状态采用定时器驱动自清理，无新更新时自动移除
 * 业务方可通过 SyncBus.remove(syncId) 手动兜底清理
 */
private static final ConcurrentHashMap<Long, Map<String, SyncThrottleState>> throttleStates =
    new ConcurrentHashMap<>();

/**
 * 移除指定实体的所有限流状态
 * 限流状态在定时发送后会自动清理，此方法作为安全兜底
 * 业务方可在实体生命周期结束时调用（如玩家下线、实体销毁）
 *
 * 线程安全说明：此方法必须在对应实体的执行器链内调用（与 sync() 在同一条串行链），
 * 以避免与定时器回调中的 removeThrottleState 产生竞态。
 *
 * @param syncId 实体唯一标识（即 ISyncHolder.getSyncId()）
 */
public static void remove(long syncId) {
    throttleStates.remove(syncId);
}
```

- 限流状态按 `(syncId, fieldName)` 索引
- 外层使用 `ConcurrentHashMap<Long, Map<String, SyncThrottleState>>`：不同实体可能从不同执行器链访问（ConcurrentHashMap 保证线程安全），以 `syncId`（即 `ISyncHolder.getSyncId()`）为 key，不依赖实体的 `equals/hashCode` 实现
- 内层 `Map<String, SyncThrottleState>` 是普通 HashMap，因为同一实体的所有 `sync()` 调用在同一执行器链内串行执行
- **内存回收采用定时器驱动自清理**：限流状态在定时器到期且无新更新时自动移除（`removeThrottleState`），无需业务显式清理。`SyncBus.remove(syncId)` 作为兜底手段保留，业务方可在实体下线等时机调用
- 不使用 WeakHashMap 是因为实体可能覆盖 `equals/hashCode`（如 Lombok `@Data`），字段变更会导致 hashCode 变化，使限流状态条目无法命中

### 9.4 限流判断流程

采用 **dirty 标记 + 定时器自清理** 模式。限流状态的存在即表示"正在限流周期中"，定时器到期后根据 dirty 标记决定是继续限流还是自清理。

当 `SyncBus.sync(holder, fieldName)` 被调用且 `syncIntervalMs > 0` 时：

```
taskKey = KeyedVirtualExecutor.currentTaskKey()   // 捕获当前线程的 TaskKey
state = getThrottleState(holder.getSyncId(), fieldName)  // 以 syncId 为 key

if state != null:
    // 正在限流周期中，标记有新更新，等待定时器处理
    state.dirty = true
    → 本次调用结束，不发送

else:
    // 无限流状态，立即发送
    → 执行发送流程（读值 → 编码 → RPC）
    // 创建限流状态并启动定时器（需要虚拟线程环境支持）
    if taskKey != null && GlobalScheduler.getInstance() != null:
        newState = new SyncThrottleState()
        putThrottleState(syncId, fieldName, newState)
        scheduleThrottleCheck(holder, module, fieldMeta, syncId, fieldName,
                              newState, taskKey, syncIntervalMs)

// ---- scheduleThrottleCheck 定时器逻辑（自递归） ----
GlobalScheduler.schedule(taskKey, () -> {
    if state.dirty:
        // 有新更新，发送最新值并重新定时
        state.dirty = false
        → 读取此刻最新字段值（MethodHandle） → 编码 → RPC 发送
        scheduleThrottleCheck(...)   // 递归调度下一轮
    else:
        // 无新更新，移除限流状态（自清理）
        removeThrottleState(syncId, fieldName)
}, syncIntervalMs, TimeUnit.MILLISECONDS)
```

> **为什么用 TaskKey 而非硬编码 TaskModule.PLAYER**：`sync()` 可能在不同模块的执行器链中被调用，使用 `currentTaskKey()` 捕获调用现场的线程标识，确保延迟任务回到同一条串行链执行，保证字段读取与业务修改不会并发。

> **定时器自清理**：当定时器到期发现 `dirty == false`（即限流周期内没有新的 `sync()` 调用），说明该字段不再被频繁修改，自动移除限流状态。下次 `sync()` 调用时将重新立即发送并创建新的限流周期。这避免了传统 `lastSyncTimeMs` 方案需要业务手动清理的内存管理负担。

### 9.5 时序示例

假设 `allianceId` 的 `syncInterval = 1`（1 秒）：

```
T=0.0s  sync("allianceId")  → 无限流状态，立即发送（value=A），创建状态，定时器 +1s
T=0.2s  sync("allianceId")  → 有状态，标记 dirty=true
T=0.5s  sync("allianceId")  → 有状态，dirty 已为 true，无额外操作
T=0.7s  sync("allianceId")  → 有状态，dirty 已为 true，无额外操作
T=1.0s  定时器触发            → dirty=true，读取最新值（value=D），发送，dirty=false，定时器 +1s
T=2.0s  定时器触发            → dirty=false，无新更新 → 移除限流状态（自清理）
T=3.5s  sync("allianceId")  → 无限流状态，立即发送（value=E），创建状态，定时器 +1s
T=4.5s  定时器触发            → dirty=false → 移除限流状态（自清理）
```

### 9.6 syncAll 与限流

`SyncBus.syncAll(holder)` **与 sync() 的限流机制完全独立**：

- syncAll 直接对所有 `@SyncField` 字段立即读值并发送，**不经过限流判断**
- syncAll **不会重置** sync 的限流状态（`dirty` 标记和定时器均不变）
- 若此时有定时器在等待，它仍会在到期后正常检查 dirty 标记并处理（与 syncAll 发送的数据可能重复，但不影响正确性，属于幂等操作）
- 两套机制互不干扰：syncAll 是"立即全量快照"，sync 是"增量限流同步"

典型使用场景（具体时机由业务自行判断）：
- Cache 端实体刚创建，需要全量同步初始数据
- 断线重连后补偿同步
- 跨场景迁移后数据补全

### 9.7 延迟发送中 Holder 引用的持有

定时器回调的 lambda 持有 Holder 实体的引用（`ISyncHolder` 实例），用于在到期时通过 MethodHandle 读取最新字段值。

这是安全的：
- Holder 实体（如 PlayerEntity）的生命周期远长于限流间隔（秒级）
- 通过 `currentTaskKey()` 捕获的 TaskKey 调度定时器，到期后回到发起同步的同一条执行器链串行执行，与业务操作不会并发
- 定时器采用自递归模式：有更新则重新调度，无更新则自动退出并移除限流状态，不会无限持有引用

### 9.8 限流状态的清理

限流状态采用**定时器驱动自清理**机制：当定时器到期发现 `dirty == false` 时，自动移除该字段的限流状态（`removeThrottleState`），若 syncId 下已无任何字段状态，整个 syncId 条目也会被移除。因此大多数场景下无需业务手动清理。

`SyncBus.remove(syncId)` 作为**安全兜底**保留，业务方可在实体生命周期结束时调用。

典型调用时机（由业务自行判断）：
- 玩家下线时：`SyncBus.remove(playerId)`
- 实体从管理器移除时
- 进程关闭前的清理阶段

> **线程安全约束**：`remove(syncId)` 必须在对应实体的执行器链内调用（与 `sync()` 在同一条串行链），以避免与定时器回调中的 `removeThrottleState` 产生竞态。通常在玩家下线流程中调用，自然满足该约束。

若实体已被 `remove` 但仍有定时器未触发：
- 定时器到期后仍会执行（lambda 持有 Holder 实例的强引用）
- 若 `dirty == true`，会发送一次最新值并重新调度，最终在下一轮 `dirty == false` 时自清理退出
- 由于实体已下线，后续不会再有新的 `sync()` 调用，定时器最终会在 1-2 轮后自动停止

## 十、文件清单

### 新增文件（共 14 个）

| 文件 | 包路径 | 说明 |
|------|--------|------|
| `SyncModule` | `syncbus/` | 同步模块枚举（显式 ID） |
| `SyncBus` | `syncbus/` | 同步总线入口（sync/syncAll + 限流状态管理） |
| `ISyncCacheResolver` | `syncbus/` | 缓存查找器接口 |
| `SyncEntity` | `syncbus/anno/` | 类级别注解，声明所属 SyncModule |
| `SyncField` | `syncbus/anno/` | 同步字段注解（含 encoder/decoder/syncInterval 属性） |
| `ISyncFieldEncoder` | `syncbus/codec/` | 字段编码器接口（Holder 端实现） |
| `ISyncFieldDecoder` | `syncbus/codec/` | 字段解码器接口（Cache 端实现） |
| `SyncHolderFieldMeta` | `syncbus/model/` | Holder 端字段元数据 |
| `SyncHolderMeta` | `syncbus/model/` | Holder 端实体元数据 |
| `SyncCacheFieldMeta` | `syncbus/model/` | Cache 端字段元数据 |
| `SyncCacheMeta` | `syncbus/model/` | Cache 端实体元数据 |
| `SyncBusRegistry` | `syncbus/core/` | 扫描注册中心（注解驱动，无实例化） |
| `SyncBusRpcService` | `syncbus/core/` | RPC 接收端实现 |
| `ISyncBusRpcService` | `rpc/impl/syncbus/` | RPC 接口定义 |

> 以上包路径均以 `com.slg.net.` 为前缀

### 修改文件（共 2 个）

| 文件 | 修改内容 |
|------|----------|
| [ISyncHolder.java](slg-net/src/main/java/com/slg/net/syncbus/ISyncHolder.java) | 增加 `getSyncId()` 方法 |
| [ISyncCache.java](slg-net/src/main/java/com/slg/net/syncbus/ISyncCache.java) | 增加 `getSyncId()` 和 `onSyncUpdated()` 方法 |

### 包结构总览

```
com.slg.net.syncbus/                      根包：业务直接使用的核心 API
├── SyncModule.java                       同步模块枚举（显式 ID）
├── SyncBus.java                          同步总线入口
├── ISyncHolder.java                      主体方接口（已存在，增加 getSyncId）
├── ISyncCache.java                       缓存方接口（已存在，增加 getSyncId/onSyncUpdated）
├── ISyncCacheResolver.java               缓存查找器接口
├── anno/                                 注解（对齐 rpc/anno/）
│   ├── SyncEntity.java                   类级别注解：声明所属 SyncModule
│   └── SyncField.java                    字段级别注解：标记同步字段
├── codec/                                编解码器接口（对齐 message/core/codec/）
│   ├── ISyncFieldEncoder.java
│   └── ISyncFieldDecoder.java
├── model/                                元数据模型（对齐 rpc/model/）
│   ├── SyncHolderFieldMeta.java          Holder 端字段元数据
│   ├── SyncHolderMeta.java               Holder 端实体元数据
│   ├── SyncCacheFieldMeta.java           Cache 端字段元数据
│   └── SyncCacheMeta.java                Cache 端实体元数据
└── core/                                 内部实现
    ├── SyncBusRegistry.java              扫描注册中心
    └── SyncBusRpcService.java            RPC 接收端实现

com.slg.net.rpc.impl.syncbus/            RPC 接口（对齐 rpc/impl/scene/）
└── ISyncBusRpcService.java
```

## 十一、v2 → v3 变更摘要

| 变更项 | v2 | v3 |
|--------|----|----|
| 模块归属声明 | `getSyncModule()` 实例方法 | `@SyncEntity` 类注解 |
| SyncModule ID | `ordinal()` | 显式 `id` 字段 |
| 启动扫描 | 临时实例化获取模块 | 纯注解读取，零实例化 |
| 跨进程配对校验 | 启动时校验字段名/类型，不匹配阻止启动 | 不做配对校验，运行时 error 日志 |
| 元数据模型 | `SyncFieldMeta` + `SyncEntityMeta`（混合两端） | Holder/Cache 各自独立元数据 |
| ISyncHolder 接口 | `getSyncId` + `getSyncModule` + `syncTargetServerIds` | `getSyncId` + `syncTargetServerIds`（去掉 getSyncModule） |
| ISyncCache 接口 | `getSyncId` + `getSyncModule` + `onSyncUpdated` | `getSyncId` + `onSyncUpdated`（去掉 getSyncModule） |
| fire-and-forget | 建议关键节点 syncAll 补偿 | 不对异常数据丢失做出反应，建议业务自行判断时机调用 syncAll 补偿 |
| 同步限流 | 无 | `@SyncField(syncInterval=N)` 按字段限流，默认 1 秒。采用 dirty 标记 + 定时器自递归自清理模式 |
| syncAll 与限流 | — | syncAll 与 sync 完全独立，不重置限流状态，不影响定时器 |
| fieldData 格式 | — | 统一为 `Map<String, String>`，所有字段值编码为 String，避免双重编码 |
| 限流状态内存管理 | — | 使用 `ConcurrentHashMap<Long, ...>` 以 syncId 为 key，定时器驱动自清理，`SyncBus.remove(syncId)` 作为兜底 |
| 新增文件数 | 11 | 14（元数据拆分为 4 个 + 新增 @SyncEntity 注解） |
