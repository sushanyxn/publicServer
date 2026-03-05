# 框架集成测试结果（slg-framework-test）

**执行时间**: 2026-03-05 17:23  
**范围**: 集成测试模块 `slg-framework-test`（端到端、性能、压力测试，依赖 Testcontainers Redis/MySQL）  
**环境**: Windows 10, Maven 3.x, JDK 21, Docker Desktop 4.52.0 (context: default)

## 汇总

| 指标     | 数值 |
|----------|------|
| 用例总数 | 18   |
| 通过     | 18   |
| 失败     | 0    |
| 错误     | 0    |
| 跳过     | 0    |

**BUILD SUCCESS**，总耗时 01:31 min

## 各测试类详情

| 测试类                          | 类型   | 用例数 | 通过 | 耗时      |
|--------------------------------|--------|--------|------|-----------|
| PersistenceE2EIntegrationTest  | E2E    | 4      | 4    | 18.62s    |
| RedisCacheE2EIntegrationTest   | E2E    | 4      | 4    | 2.651s    |
| RedisRouteE2EIntegrationTest   | E2E    | 2      | 2    | 2.593s    |
| PersistencePerformanceTest     | 性能   | 2      | 2    | 28.08s    |
| RedisCachePerformanceTest      | 性能   | 2      | 2    | 3.324s    |
| PersistenceStressTest          | 压力   | 2      | 2    | 25.45s    |
| RedisCacheStressTest           | 压力   | 2      | 2    | 1.443s    |

## 性能指标

| 指标                           | 结果                                    |
|-------------------------------|-----------------------------------------|
| 持久化 insert 100 条（提交到队列） | 8ms, 约 12,500 条/秒                   |
| 持久化 findById 延迟            | 约 20ms                                 |
| Redis set 2000 次              | 1,325ms, 约 1,509 次/秒                 |
| Redis get 1000 次              | 764ms, 约 1,309 次/秒, P99 延迟 1,446μs |

## 与上次结果对比

| 项目                    | 上次 (2026-03-05 16:11) | 本次 (2026-03-05 17:23) |
|------------------------|------------------------|------------------------|
| 通过 / 总数             | 0 / 7                  | 18 / 18                |
| 失败原因               | Docker 不可用（Testcontainers 无法连接） | 无                      |

### 本次修复的问题

1. **Docker context**：上次使用 `desktop-linux` context（管道 `dockerDesktopLinuxEngine`），Testcontainers 返回 400；本次切换为 `default` context（管道 `docker_engine`），连接正常。
2. **FrameworkTestRedisOnlyApplication 扫描范围过大**：扫描了 `com.slg.net` 包，导致 `RpcClientConfiguration` 被加载并要求配置 `rpc.client.route-service-class`。修复：移除 `com.slg.net` 扫描（Redis-only 测试不需要 net 包）。
3. **持久化测试缺少 Redis 容器**：`FrameworkTestApplication` 扫描 `com.slg.redis`，`RedisLifeCycleConfiguration` 启动时校验 Redis 连接。持久化测试只配置了 MySQL 容器。修复：为 3 个持久化测试类添加 Redis Testcontainer。
4. **FrameworkTestRedisRouteApplication 扫描范围过大**：扫描 `com.slg.net` 引入 WebSocket 服务器等不需要的组件。修复：缩小为 `com.slg.net.rpc`，排除 `RpcClientConfiguration`。
5. **IRouteSupportService bean 缺失**：Redis Route 测试需要 `IRouteSupportService`。修复：`TestRpcRouteSupportService` 同时实现 `IRouteSupportService` 和 `IRpcRouteSupportService`。

## 执行命令

```bash
$env:DOCKER_HOST='npipe:////./pipe/docker_engine'; mvn test -pl slg-framework-test
```

## 涉及修改的文件

- `slg-framework-test/src/test/java/.../FrameworkTestRedisOnlyApplication.java`：移除 `com.slg.net` 扫描
- `slg-framework-test/src/test/java/.../FrameworkTestRedisRouteApplication.java`：缩小为 `com.slg.net.rpc`，排除 `RpcClientConfiguration`
- `slg-framework-test/src/test/java/.../rpcroute/TestRpcRouteSupportService.java`：实现 `IRouteSupportService`
- `slg-framework-test/src/test/java/.../persistence/PersistenceE2EIntegrationTest.java`：添加 Redis 容器
- `slg-framework-test/src/test/java/.../performance/PersistencePerformanceTest.java`：添加 Redis 容器
- `slg-framework-test/src/test/java/.../stress/PersistenceStressTest.java`：添加 Redis 容器
