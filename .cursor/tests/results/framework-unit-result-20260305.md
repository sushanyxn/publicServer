# 框架单元测试结果

**执行时间**: 2026-03-05  
**范围**: 按计划开发的四类框架单元测试（仅单元测试，无集成）；补全后含 EntityCache、KeyedVirtualExecutor、GlobalScheduler、CacheEntityMeta/CacheAccessor、RpcRedisRouteConsumerRunner。

## 汇总

| 模块 | 测试类 | 结果 |
|------|--------|------|
| slg-support | PersistenceExceptionUtilTest | 通过 |
| slg-support | PersistenceRetryWrapperTest | 通过 |
| slg-support | AsyncPersistenceServiceTest | 通过 |
| slg-support | **EntityCacheTest** | 通过 |
| slg-common | TaskModuleTest | 通过 |
| slg-common | **KeyedVirtualExecutorTest** | 通过 |
| slg-common | **GlobalSchedulerTest** | 通过 |
| slg-redis | CacheModuleTest | 通过 |
| slg-redis | RedisCacheServiceTest | 通过 |
| slg-redis | **CacheEntityMetaTest** | 通过 |
| slg-redis | **CacheAccessorTest** | 通过 |
| slg-net | RedisRouteTest | 通过 |
| slg-net | RedisRoutePublisherTest | 通过 |
| slg-net | RpcRouteRedisPropertiesTest | 通过 |
| slg-net | **RpcRedisRouteConsumerRunnerTest** | 通过 |

## 执行命令

```bash
mvn test -pl slg-support   # 通过
mvn test -pl slg-common    # 通过
mvn test -pl slg-redis     # 通过
mvn test -pl slg-net      # 通过
```

## 覆盖的测试计划要点

- **持久化**: PersistenceRetryWrapper 正常/可重试/不可重试/默认重试次数；PersistenceExceptionUtil isRetryable/getShortDescription；AsyncPersistenceService 空参数不调 repository、findById(null)/findAll(null) 返回完成 future。
- **线程模型**: TaskModule toKey/toKey(long)、isMultiChain、getName（KeyedVirtualExecutor/GlobalScheduler 需 Spring 或真实 executor，本次未加）。
- **Redis 缓存**: CacheModule buildKey/fromId；RedisCacheService set/get/delete/setString/getString 及带过期 set，Mock RedisTemplate。
- **Redis Route**: RedisRoute getRouteParams、sendMsg 参数校验与调用 publisher；RedisRoutePublisher publishRaw/publishResp 调用 getRedisRouteChannel 与 execute；RpcRouteRedisProperties 默认值与 setter。

## 补全内容（本轮追加）

- **EntityCacheTest**：findById 未命中调用 findById、命中不重复调 repository；save 写缓存并调 asyncSave；evict/clear；findById(null) 不调 repository。
- **KeyedVirtualExecutorTest**：手动注入 VirtualExecutorHolder，同 key 串行、不同 key 并发、单链串行、inThread 在任务内为 true、任务抛异常后同 key 下一任务仍执行。
- **GlobalSchedulerTest**：手动注入 KeyedVirtualExecutor，schedule(delay) 与 scheduleWithFixedDelay 单链延迟执行。
- **CacheEntityMetaTest**：手动构造 Player 的 meta，buildKey/getModule/getFieldNames/getFieldMeta/validateFieldName/newInstance；CacheFieldMeta encode/decode 往返。
- **CacheAccessorTest**：Mock StringRedisTemplate#opsForHash，setField/getField/setFields/getFields 调用与编码；非法字段名抛异常。
- **RpcRedisRouteConsumerRunnerTest**：start 前 isRunning 为 false、getPhase 为正数、start 后 isRunning 为 true、stop 后为 false。

## 说明

- 各模块测试代码位于对应模块的 `src/test/java`，包名与源码一致。
- 集成测试（Testcontainers Redis/MySQL）仍可选，未在本轮实现。
