# 线程模型（Executor）测试计划

**模块**：slg-common  
**核心类**：KeyedVirtualExecutor、GlobalScheduler、VirtualExecutorHolder、TaskModule、ExecutorConstants

## 单元测试

- **TaskModuleTest**：`toKey()`/`toKey(long)`、`isMultiChain()`。
- **KeyedVirtualExecutorTest**：同 key 串行、不同 key 并发；单链串行；`inThread`；异常不阻断后续任务。
- **GlobalSchedulerTest**：schedule(delay)、scheduleWithFixedDelay 单链。

## 性能测试

**测试类**：`com.slg.common.executor.ExecutorPerformanceTest`

### 目标

测量线程框架**本身**在常规任务消耗之外带来的调度与分配开销，便于评估：

- 纯入队/调度路径的吞吐与单任务延迟；
- 单链 vs 多链、同 key vs 多 key 的差异；
- `execute` vs `submit`（CompletableFuture）的开销；
- GlobalScheduler.schedule(0) 的端到端开销。

### 场景与含义

| 场景 | 含义 | 主要度量 |
|------|------|----------|
| baseline_rawRunnableLoop | 裸 Runnable.run() 循环，无框架 | 理论最小单任务耗时（纳秒级） |
| execute_singleChain | 单链 SYSTEM 入队 + 虚拟线程 drain | 投递吞吐、平均 μs/task |
| execute_multiChainSameKey | 多链同 key (PLAYER,1L) 串行 | 同 key 串行时的框架开销 |
| execute_multiChainManyKeys | 多链 200 个 key 分散投递 | 多 key 并发下的吞吐与平均延迟 |
| submit_singleChain | submit + get() 单链 | CompletableFuture + 同步等待开销 |
| submit_multiChainManyKeys | submit + get() 多 key | CF + 多 key 的每任务延迟 |
| schedule_zeroDelay_multiChainManyKeys | schedule(0) 后转投执行器 | 调度器 + 转投的端到端开销 |

### 如何解读

- **avg μs/task**：总墙钟时间 / 任务数，表示“单任务平均耗时”（含排队与执行）。与 baseline 的 avg 相减可近似得到“框架单任务额外开销”。
- **throughput ops/s**：单位时间完成的任务数，受限于投递侧（execute 快）或等待侧（submit.get 串行等待）。

### 执行方式

```bash
mvn test -pl slg-common -Dtest=ExecutorPerformanceTest -DskipTests=false
```

结果输出在控制台，可复制到 `.cursor/tests/results/executor-result-{日期}.md` 归档。
