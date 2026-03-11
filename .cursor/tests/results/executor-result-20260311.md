# 线程模型（Executor）测试结果

**执行时间**：2026-03-11  
**模块**：slg-common  
**测试类型**：单元测试 + 性能测试

## 执行命令

```bash
mvn test -pl slg-common -Dtest=ExecutorPerformanceTest -DskipTests=false
```

## 性能测试结果（示例一次运行）

| 场景 | count | total ms | throughput ops/s | avg μs/task |
|------|-------|----------|------------------|-------------|
| baseline_rawRunnableLoop | 30000 | 0 | ~36.8M | 0.03 |
| execute_singleChain | 30000 | 6 | ~4.8M | 0.21 |
| execute_multiChainSameKey | 30000 | 5 | ~5.5M | 0.18 |
| execute_multiChainManyKeys | 30000 | 19 | ~1.56M | 0.64 |
| submit_singleChain | 30000 | 229 | ~131k | 7.65 |
| submit_multiChainManyKeys | 30000 | 402 | ~75k | 13.40 |
| schedule_zeroDelay_multiChainManyKeys | 30000 | 162 | ~185k | 5.41 |

## 简要结论

- **基线**：裸 run() 约 0.03 μs/task，作为“无框架”参考。
- **execute**：单链/同 key 约 0.18–0.21 μs/task，多 key 约 0.64 μs/task；框架在纯投递路径上增加约 0.15–0.6 μs/任务。
- **submit**：因需 `get()` 同步等待，单任务延迟约 7–13 μs，吞吐受限于串行等待。
- **schedule(0)**：调度器 + 转投约 5.4 μs/task，适合评估“定时到点后转执行器”的额外成本。

用例总数：7，通过：7，失败：0，跳过：0。
