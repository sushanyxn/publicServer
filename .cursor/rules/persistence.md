---
description: MySQL/Redis/MongoDB 持久化与缓存相关规范
globs:
  - "**/entity/**"
  - "**/cache/**"
  - "**/redis/**"
  - "**/repository/**"
  - "**/persist/**"
---

# 持久化规范

## MySQL 框架（@EnableMysql）

- 基于 JPA（EntityManager）+ EntityCache 实现，定义在 `slg-support`
- **禁止使用 `JpaRepository`**（Spring Data JPA Repository 接口）
- **禁止直接操作 `EntityManager`**
- 实体必须继承 `BaseMysqlEntity<ID>`
- 实体必须实现 `save()` 和 `saveField()` 方法，通过 Service 的 `getInstance()` 回调 EntityCache
- 使用 `@EntityCacheInject` 注入 `EntityCache<T>` 实例
- 使用 `@CacheConfig` 配置缓存参数
- 复杂类型使用 `@Serialized` + `@Column(columnDefinition = "json")`

## Redis 模块（slg-redis）

- 独立二级模块，通过 Maven 依赖引入即自动启用
- **禁止直接操作 `RedisTemplate`**，所有缓存业务通过 `CacheAccessor` 进行
- 使用 `@CacheEntity` 声明所属 `CacheModule`，`@CacheField` 标记参与缓存的字段
- 使用 `@CacheAccessorInject` 按泛型自动注入 `CacheAccessor`

## MongoDB（@EnableMongo）

- 定义在 `slg-support`，依赖 optional，使用模块需显式引入并标注 `@EnableMongo`

## 数据库选择注解

| 注解 | 数据库 | 使用模块 |
|------|--------|---------|
| `@EnableMysql` | MySQL | slg-web、slg-log |
| `@EnableMongo` | MongoDB | slg-game、slg-scene、slg-singlestart |
| `slg-redis` 依赖 | Redis | 需要缓存的模块 |
| `@EnableZookeeper` | ZooKeeper | 需要配置/服务注册的模块 |
