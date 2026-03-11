# Docker 本地依赖

本目录提供 Redis、MySQL、MongoDB、Zookeeper、Elasticsearch 等中间件的 docker-compose 编排，供本地开发与联调使用。各子目录内 `docker-compose.yml` 已配置端口、卷、健康检查与日志上限。

## 子目录与端口

| 目录 | 服务 | 主机端口 | 说明 |
|------|------|----------|------|
| redis | slg-redis | 6379 | 业务 Redis |
| redis-route | slg-redis-route | 6380 | RPC 路由 Redis |
| mysql | slg-mysql | 3306 | MySQL（root/123456） |
| mongodb | slg-mongodb | 27017 | MongoDB |
| zookeeper | slg-zookeeper | 2181, 28080 | ZK 服务与管理端 |
| zoonavigator | slg-zoonavigator | 9000 | ZK 可视化管理（随 docker/zookeeper 一起启动） |
| elasticsearch | slg-es01, slg-es02 | 9200, 9300 | ES 集群（仅 es01 暴露主机端口） |

## 常用命令

- 启动：`docker compose up -d`
- 停止：`docker compose down`
- 停止并删卷：`docker compose down -v`

## 常见问题：本机连不上端口（端口未发布）

**现象**：容器是 Up 的，但本机 `localhost:端口` 连接被拒绝；`docker ps` 里该容器只显示 `端口/tcp`，没有 `0.0.0.0:端口->端口/tcp`。

**原因**：  
首次 `docker compose up -d` 时若端口已被占用，Compose 会**创建**容器但**启动失败**。之后端口空闲时再执行 `up -d`，Compose 可能直接**启动之前未成功启动的容器**，在部分环境下该容器不会重新应用端口映射，导致端口未绑定到主机。

**解决**（在对应子目录执行）：

```bash
docker compose down
docker compose up -d --force-recreate
```

`--force-recreate` 会按当前 compose 配置重新创建容器，端口会正确发布。数据在命名卷中，不会因重建容器而丢失。

各 `docker-compose.yml` 顶部注释里也已写明：若本机连不上对应端口，可先 `down` 再 `up -d --force-recreate`。

## Docker Desktop 配置备份

- `desktop-config/`：本机 Docker Desktop 相关配置的只读备份（`settings-store.json`、`config.json`、`daemon.json` 等），详见该目录下 `README.md`。仅作参考与还原参考，勿直接覆盖系统配置。

## 其他说明

- 日志：所有服务已配置 `logging.options.max-size: "50m"`, `max-file: "3"`，避免日志占满磁盘。
- Zookeeper：健康检查已适配 3.9+（使用 `srvr` 四字命令）；管理端口映射为 28080 避免与主机 8080 冲突。
- 生产环境：建议使用托管中间件或自建集群，勿直接沿用本编排的默认账号与单点配置。
