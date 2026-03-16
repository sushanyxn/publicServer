# Redis Route 吞吐优化方案

## 一、现状分析

### 1.1 当前性能基线

| 指标 | 数值 | 来源 |
|------|------|------|
| 单链路顺序写入 | ~1,500~1,800 ops/s | `RedisRoutePerformanceTest` 场景一 |
| 多链路并发聚合 | ~40,000 ops/s (50线程) | `RedisRoutePerformanceTest` 场景二 |
| 单次 XADD P99 延迟 | ~1,034 μs | 场景一补充（1KB） |
| 双向互写聚合吞吐 | ~3,693 ops/s | 场景三 |

### 1.2 瓶颈定位

```
┌──────────────────────────────────────────────────────────────────────┐
│  RedisRoutePublisher.xadd()  — 单条消息完整调用链                      │
├──────────────────────────────────────────────────────────────────────┤
│  ① encode (MessageWireCodec)    ~20μs    │ 编码 + ByteBuf 分配      │
│  ② streamKey.getBytes()          ~1μs    │                          │
│  ③ 构造 MapRecord + XAddOptions  ~5μs    │                          │
│  ④ RedisTemplate.execute()    ~550μs     │ ← 主瓶颈：获取连接 →      │
│     → Lettuce XADD              ~30μs    │    发送命令 → 等待响应     │
│     → 网络 RTT                  ~500μs   │    → 释放连接             │
└──────────────────────────────────────────────────────────────────────┘
  总计 ~580μs/msg → ~1,700 ops/s
```

**核心瓶颈**：每条消息独立执行一次 `RedisTemplate.execute(RedisCallback)`，整个调用是同步阻塞的。单次网络 RTT 约 0.5ms，占总耗时 85% 以上。Redis 服务端 XADD 处理仅需几十微秒，大量时间浪费在等待网络响应上。

### 1.3 涉及核心类

| 类 | 模块 | 职责 |
|----|------|------|
| `RedisRoutePublisher` | slg-net | 发布消息到目标 Stream（XADD） |
| `RpcRedisRouteConsumerRunner` | slg-net | 从本服 Stream 消费消息（XREADGROUP） |
| `RouteRedisAutoConfiguration` | slg-net | 创建转发专用 Redis 连接工厂和 Template |
| `RpcRouteRedisProperties` | slg-net | `rpc.route.redis.*` 配置属性 |
| `MessageWireCodec` | slg-net | 消息编解码 |

---

## 二、优化方案

### P0：Pipeline 批量发送 + Lettuce 异步 API

**预期提升**：5~20 倍（目标 10,000~30,000 ops/s 单链路）

#### 2.1 设计思路

将逐条同步 XADD 改为本地缓冲 + 批量 Pipeline 刷新，一次网络往返发送 N 条命令：

```
当前：  msg1 → RTT → msg2 → RTT → msg3 → RTT     (3 × RTT)
优化后：msg1,msg2,msg3 → RTT → ack1,ack2,ack3      (1 × RTT)
```

#### 2.2 实现要点

**新增 `PipelinedRedisRoutePublisher` 替代当前 `RedisRoutePublisher`**：

1. **本地缓冲区**：`ConcurrentLinkedQueue<PendingMessage>`，每条 `publishRaw()` 入队后立即返回
2. **刷新触发条件**（满足任一即刷新）：
   - 缓冲区消息数 ≥ `batchSize`（默认 50）
   - 缓冲区累计字节数 ≥ `batchMaxBytes`（默认 512KB）
   - 距上次刷新超过 `flushIntervalMs`（默认 2ms）
3. **刷新执行**：
   - 获取 Lettuce 原生 `StatefulRedisConnection`，持有为成员变量（非每次获取）
   - `async.setAutoFlushCommands(false)` → 逐条 `async.xadd(...)` → `async.flushCommands()` → 等待所有 Future
4. **优雅停机**：`SmartLifecycle.stop()` 时强制刷新剩余消息

**为什么需要字节数限制**：仅按条数触发，如果一批消息都是大包体（如 16KB），50 条 × 16KB = 800KB 会一次性压入 Lettuce 发送缓冲区。虽然 Redis 输入缓冲区上限 1GB 远超此量级，但大批次会增加 Redis 单线程的连续处理时间，对同连接后续命令产生排队延迟，也增大 JVM 缓冲区暂存的内存压力。双重限制在常规小消息场景下由条数触发（高效），在大消息场景下由字节数兜底（安全）。

```java
public class PipelinedRedisRoutePublisher {

    private final ConcurrentLinkedQueue<PendingMessage> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingBytes = new AtomicInteger(0);
    private final RedisAsyncCommands<byte[], byte[]> asyncCommands;
    private final ScheduledExecutorService flusher; // 定时刷新线程

    private final int batchSize;        // 默认 50
    private final int batchMaxBytes;    // 默认 512KB
    private final long flushIntervalMs; // 默认 2ms

    public void publishRaw(int targetServerId, byte[] data) {
        buffer.offer(new PendingMessage(buildStreamKey(targetServerId), data));
        int totalBytes = pendingBytes.addAndGet(data.length);
        if (buffer.size() >= batchSize || totalBytes >= batchMaxBytes) {
            flush();
        }
    }

    private void flush() {
        asyncCommands.setAutoFlushCommands(false);
        try {
            List<RedisFuture<String>> futures = new ArrayList<>();
            PendingMessage msg;
            while ((msg = buffer.poll()) != null) {
                futures.add(asyncCommands.xadd(msg.key, msg.buildArgs()));
            }
            pendingBytes.set(0);
            asyncCommands.flushCommands();
            LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[0]));
        } finally {
            asyncCommands.setAutoFlushCommands(true);
        }
    }
}
```

#### 2.3 配置项扩展

在 `RpcRouteRedisProperties` 中新增：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `rpc.route.redis.pipeline-batch-size` | 50 | Pipeline 刷新条数阈值 |
| `rpc.route.redis.pipeline-batch-max-bytes` | 524288 (512KB) | Pipeline 刷新字节数阈值，防止大消息批次过大 |
| `rpc.route.redis.pipeline-flush-interval-ms` | 2 | 定时刷新间隔（毫秒） |
| `rpc.route.redis.pipeline-enabled` | true | 是否启用 Pipeline 模式 |

#### 2.4 对有序性的影响

**无影响**。同一 Lettuce 连接上的命令按 `flushCommands()` 时的队列顺序发送到 Redis，Redis 单线程串行执行。同一 Stream key 中的消息顺序与 `buffer.poll()` 出队顺序一致，即发送方 `publishRaw()` 的调用顺序。

#### 2.5 对可靠性的影响

- `awaitAll` 等待所有 Future 完成 → 有写入确认，不丢消息
- 若 Redis 连接断开，`flush()` 抛异常，调用方可感知
- `flushIntervalMs` 引入最多 2ms 的发送延迟，RPC 场景可接受

#### 2.6 改动范围

| 文件 | 改动 |
|------|------|
| `RedisRoutePublisher.java` | 重构为 Pipeline 模式，或新建 `PipelinedRedisRoutePublisher` |
| `RouteRedisAutoConfiguration.java` | 新增获取 Lettuce 原生 `StatefulRedisConnection` 的 Bean |
| `RpcRouteRedisProperties.java` | 新增 pipeline 相关配置项 |
| `RpcRouteConfiguration.java` | 注入新依赖 |

---

### P1：消费端批量与延迟调优

**预期提升**：消费侧吞吐 1.5~2 倍，尾延迟显著降低

#### 3.1 调整 `RpcRouteRedisProperties` 默认值

| 属性 | 当前默认 | 建议默认 | 说明 |
|------|----------|----------|------|
| `batchSize` | 10 | 50 | 单次 XREADGROUP 最大条数，减少 Redis 往返 |
| `blockSeconds` | 1 | — | 改为毫秒级配置 |

新增配置项：

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `rpc.route.redis.block-millis` | 200 | 阻塞读取超时（毫秒），取代 `blockSeconds` |

#### 3.2 消费者批量 ACK + DELETE

当前逐条 `ackAndDelete`，可改为批量：

```java
// 当前：逐条
for (MapRecord record : records) {
    processRecord(record, ...);
}

// 优化：收集 ID → 批量 ACK + 批量 DELETE
List<RecordId> ids = new ArrayList<>();
for (MapRecord record : records) {
    processMessage(record);
    ids.add(record.getId());
}
streamOps.acknowledge(streamKey, group, ids.toArray(new RecordId[0]));
streamOps.delete(streamKey, ids.toArray(new RecordId[0]));
```

#### 3.3 改动范围

| 文件 | 改动 |
|------|------|
| `RpcRouteRedisProperties.java` | 新增 `blockMillis`，标记 `blockSeconds` deprecated |
| `RpcRedisRouteConsumerRunner.java` | 使用 `blockMillis`；批量 ACK/DELETE |

---

### P2：MessageWireCodec 编码优化

**预期提升**：~10~20%（减少 GC 压力，高频场景下更明显）

#### 4.1 ThreadLocal ByteBuf 复用

当前每次 `encode()` 分配两个 `Unpooled.buffer()`。改为 ThreadLocal 复用：

```java
private static final ThreadLocal<ByteBuf> BODY_BUF = ThreadLocal.withInitial(() -> Unpooled.buffer(256));
private static final ThreadLocal<ByteBuf> OUT_BUF = ThreadLocal.withInitial(() -> Unpooled.buffer(512));

public static byte[] encode(Object message) {
    ByteBuf bodyBuf = BODY_BUF.get();
    bodyBuf.clear(); // 重用，不重新分配
    // ...编码...
    ByteBuf out = OUT_BUF.get();
    out.clear();
    // ...组装...
}
```

#### 4.2 注意事项

- 虚拟线程场景下 ThreadLocal 对不同 carrier 线程各持有一份缓冲区，内存开销可控
- `clear()` 不释放底层内存，只是重置读写指针
- 若消息体超过初始容量，ByteBuf 会自动扩容（仅在需要时）

#### 4.3 改动范围

| 文件 | 改动 |
|------|------|
| `MessageWireCodec.java` | ThreadLocal 缓冲区复用 |

---

### P3：Redis 服务端 IO 线程

**预期提升**：~20~30%（配合 Pipeline 大批量传输时效果更好）

#### 5.1 修改 redis.conf

在 `docker/redis-route/redis.conf` 中新增：

```
# 启用 IO 多线程（Redis 7+ 默认关闭）
# 4 线程适用于 4~8 核 CPU，按实际调整
io-threads 4
io-threads-do-reads yes
```

#### 5.2 说明

- Redis 命令执行仍为单线程，不影响有序性和原子性
- IO 线程只加速 RESP 协议的读取/序列化，减少主线程在网络 IO 上的耗时
- Pipeline 批量发送时单次传输数据量大，IO 线程的收益更明显
- 建议 `io-threads` 设为 CPU 核心数的一半，最少 4

#### 5.3 改动范围

| 文件 | 改动 |
|------|------|
| `docker/redis-route/redis.conf` | 新增 2 行 IO 线程配置 |

---

### P4：Fire-and-Forget 模式（void 方法自动启用）

**预期提升**：void 类 RPC 调用达到极致吞吐（10 万+ ops/s），非 void 调用仍走可靠确认

#### 6.1 判定规则

**不增加额外注解字段**，直接依据 `@RpcMethod` 方法的返回值类型自动判定：

| 方法返回值 | 传输模式 | 原因 |
|-----------|---------|------|
| `void` | Fire-and-Forget | 发送方不期望返回值，`callBackId = 0`，无回调注册，即使 XADD 静默失败也无处上报异常 |
| `CompletableFuture<T>` 或其他非 void | 可靠确认（awaitAll） | 发送方注册了回调等待响应，若 XADD 丢失会导致回调永远超时 |

**依据**：当前 `RpcRemoteManager.invokeRemote()` 中已有 void 判定逻辑：

```java
boolean hasReturnValue = hasReturnValue(meta.getMethod());
long callBackId = hasReturnValue ? generateCallBackId() : 0;
```

void 方法的 `callBackId = 0`，接收方 `RpcRedisFacade` 不会发回响应。发送方在 `invokeRemote()` 中也不会注册回调、不会创建 Future。因此即使 XADD 写入失败：
- 发送方无感知（没有 Future 可以 completeExceptionally）
- 接收方无感知（消息没到，不会执行）
- 不会产生悬挂的回调或超时异常

这使得 void 方法天然适合 fire-and-forget：**写入确认带来的可靠性收益为零**（发送方已经不看结果了），而放弃确认可以显著减少 flush 阶段的等待时间。

如果未来有 void 方法需要强制可靠投递的场景，可以再考虑在 `@RpcMethod` 中增加 `reliable` 属性做特例覆盖。当前阶段按返回值类型自动判定足够。

#### 6.2 实现

在 `PipelinedRedisRoutePublisher` 中分双队列，flush 时合并发出但只等待可靠队列的 Future：

```java
public class PipelinedRedisRoutePublisher {

    private final ConcurrentLinkedQueue<PendingMessage> reliableBuffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PendingMessage> fireAndForgetBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingBytes = new AtomicInteger(0);

    /**
     * 发布消息（可靠模式）— 用于有返回值的 RPC 方法
     * flush 时等待 Redis 写入确认
     */
    public void publish(int targetServerId, byte[] data) {
        reliableBuffer.offer(new PendingMessage(buildStreamKey(targetServerId), data));
        int totalBytes = pendingBytes.addAndGet(data.length);
        if (reliableBuffer.size() + fireAndForgetBuffer.size() >= batchSize || totalBytes >= batchMaxBytes) {
            flush();
        }
    }

    /**
     * 发布消息（fire-and-forget 模式）— 用于 void RPC 方法
     * flush 时不等待写入确认，与可靠消息共享同一次 flushCommands() 网络往返
     */
    public void publishFireAndForget(int targetServerId, byte[] data) {
        fireAndForgetBuffer.offer(new PendingMessage(buildStreamKey(targetServerId), data));
        int totalBytes = pendingBytes.addAndGet(data.length);
        if (reliableBuffer.size() + fireAndForgetBuffer.size() >= batchSize || totalBytes >= batchMaxBytes) {
            flush();
        }
    }

    private void flush() {
        asyncCommands.setAutoFlushCommands(false);
        try {
            // 可靠消息 — 收集 Future
            List<RedisFuture<String>> futures = new ArrayList<>();
            PendingMessage msg;
            while ((msg = reliableBuffer.poll()) != null) {
                futures.add(asyncCommands.xadd(msg.key, msg.buildArgs()));
            }
            // fire-and-forget 消息 — 不收集 Future，搭便车一起发出
            while ((msg = fireAndForgetBuffer.poll()) != null) {
                asyncCommands.xadd(msg.key, msg.buildArgs());
            }
            pendingBytes.set(0);
            asyncCommands.flushCommands(); // 一次网络往返发出所有命令
            if (!futures.isEmpty()) {
                LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[0]));
            }
        } finally {
            asyncCommands.setAutoFlushCommands(true);
        }
    }
}
```

**关键设计**：两种消息在同一次 `flushCommands()` 中一起发出（共享网络往返），但只有 reliable 消息阻塞等待确认。当一个批次全为 void 消息时，flush 完全不阻塞；混合批次中 fire-and-forget 消息"搭便车"发出，不增加额外等待。

#### 6.3 调用链路适配

`RpcRemoteManager.invokeRemote()` 中按返回值类型选择发布方法：

```java
// 在 sendMsg / route 层传递 hasReturnValue 标识
if (hasReturnValue) {
    redisRoutePublisher.publish(targetServerId, encoded);
} else {
    redisRoutePublisher.publishFireAndForget(targetServerId, encoded);
}
```

传递路径：`RpcRemoteManager` → `AbstractRpcRoute.sendMsg()` → `RedisRoute` → `RedisRoutePublisher`。
需要在 `sendMsg` 方法签名中增加 `boolean hasReturnValue` 参数（或通过 `RpcMethodMeta` 传递）。

#### 6.4 RedisRoute 中需要添加的说明

在 `RedisRoute` 类的 JavaDoc 或关键方法注释中，需明确说明 fire-and-forget 行为及其适用边界：

> **传输可靠性策略**：
> - **有返回值（非 void）的 RPC 方法**：走可靠确认模式。Pipeline 批量 XADD 后等待 Redis 返回所有 RecordId，确保消息写入 Stream。若 XADD 失败则抛出异常，回调 Future 会 completeExceptionally。
> - **void RPC 方法**：走 fire-and-forget 模式。Pipeline 批量 XADD 后不等待确认，发送方无法感知写入是否成功。这是安全的，因为 void 方法的 callBackId = 0，发送方不注册回调也不持有 Future，即使消息丢失也不会产生悬挂状态。
> - **适用前提**：void 方法的业务语义应当是"尽力而为"的通知或触发，而非关键状态变更。如果某个 void 方法需要保证投递可靠性（如跨服扣资源），应将返回值改为 `CompletableFuture<Void>` 以强制走可靠确认路径。

#### 6.5 改动范围

| 文件 | 改动 |
|------|------|
| `PipelinedRedisRoutePublisher.java` | 双队列 + 分模式 flush |
| `AbstractRpcRoute.java` / `RedisRoute.java` | sendMsg 传递 hasReturnValue，补充 JavaDoc 说明 |
| `RpcRemoteManager.java` | 按返回值类型选择发布方法 |

---

## 三、预期效果

### 优化后性能估算

| 方案组合 | 单链路 OPS | 延迟影响 | 有序性 | 可靠性 |
|----------|-----------|----------|--------|--------|
| 当前 | ~1,500 | — | ✓ | ✓ |
| P0 Pipeline (batch=50) | ~30,000 | +2ms (max) | ✓ | ✓ |
| P0 + P1 消费优化 | ~30,000 发 / 消费吞吐翻倍 | 消费延迟降低 | ✓ | ✓ |
| P0 + P1 + P2 编码优化 | ~35,000 | 同上 | ✓ | ✓ |
| P0~P3 全部 | ~40,000+ | 同上 | ✓ | ✓ |
| P0~P4 含 Fire-and-Forget | 100,000+ | 近零 | ✓ | △ 可能丢消息 |

### 对比图

```
OPS (单链路 A→B)
│
100K ┤                                              ████ P0~P4
     │
 40K ┤                                    ████████ P0~P3
 35K ┤                              ██████ P0+P1+P2
 30K ┤                        ██████ P0 Pipeline
     │
  5K ┤
  1.5K ┤  ████ 当前
     └────────────────────────────────────────────────────
```

---

## 四、实施计划

### 第一阶段：P0 Pipeline + P4 Fire-and-Forget（核心改动）

P0 和 P4 在 Publisher 层是同一次重构，合并实施：

1. `RpcRouteRedisProperties` 新增 pipeline 配置项（含 `batchMaxBytes`）
2. `RouteRedisAutoConfiguration` 新增 Lettuce 原生连接 Bean
3. 重构 `RedisRoutePublisher` → `PipelinedRedisRoutePublisher`：双队列 + 条数/字节数双重触发 + 分模式 flush
4. `AbstractRpcRoute` / `RedisRoute` 的 `sendMsg` 传递 `hasReturnValue`，`RedisRoute` 类注释补充传输可靠性策略说明
5. `RpcRemoteManager` 按返回值类型选择 `publish` / `publishFireAndForget`
6. 单元测试：Mock 验证条数触发、字节数触发、定时触发、void/非void 分流、优雅停机清空缓冲
7. 集成测试：`RedisRouteE2EIntegrationTest` 适配新实现，验证 void 和非 void 路径功能不变
8. 性能测试：`RedisRoutePerformanceTest` 增加 Pipeline + fire-and-forget 场景，对比优化前后

### 第二阶段：P1 消费端调优

1. `RpcRouteRedisProperties` 新增 `blockMillis`
2. `RpcRedisRouteConsumerRunner` 改为批量 ACK/DELETE
3. 集成测试验证消费端正确性

### 第三阶段：P2 + P3 辅助优化

1. `MessageWireCodec` ThreadLocal 缓冲区
2. `redis.conf` IO 线程配置
3. 回归全部测试

### 第四阶段：性能验证

1. 运行完整 `RedisRoutePerformanceTest`，记录优化后各场景数据
2. 与本文档「一、现状」对比，验证达到预期目标
3. 结果归档到 `.cursor/tests/results/`

---

## 五、风险与注意事项

| 风险 | 应对 |
|------|------|
| Pipeline 引入最多 `flushIntervalMs` 的发送延迟 | 默认 2ms，对 RPC 超时（通常秒级）影响可忽略；可按场景调整 |
| 大消息批次占用过多内存或阻塞 Redis | `batchMaxBytes`（默认 512KB）字节数上限兜底，与条数限制双重触发 |
| Lettuce 原生 API 绕过 Spring Data Redis 抽象 | 封装在 Publisher 内部，对外接口不变 |
| `setAutoFlushCommands(false)` 忘记恢复 | 在 finally 块中恢复；或使用独立连接专做 Pipeline |
| 虚拟线程 + ThreadLocal ByteBuf 内存 | carrier 线程数有限（默认 CPU 核数），内存可控 |
| `blockMillis` 过小导致空轮询 CPU 开销 | 200ms 已足够；不应低于 50ms |
| void 方法 fire-and-forget 丢消息 | void 方法发送方不持有 Future、不注册回调，丢失不产生悬挂状态；若特定 void 方法需要保证可靠投递，应将返回值改为 `CompletableFuture<Void>` |
| void 方法含关键状态变更但被 fire-and-forget | 在 `RedisRoute` 类注释中明确说明此策略，开发时需遵守：关键操作不应声明为 void |
