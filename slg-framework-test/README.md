# slg-framework-test

底层框架**集成测试**模块，覆盖真实端到端、性能与热点压力测试。

## 依赖

- **Testcontainers**：MySQL 8.0、Redis 7，用于真实数据库/缓存环境
- **slg-support / slg-common / slg-redis / slg-net**：被测框架模块

## 测试分类

| 包/类 | 说明 |
|-------|------|
| **persistence** | 持久化端到端：Testcontainers MySQL + AsyncPersistenceService + BaseMysqlRepository |
| **redis** | Redis 缓存端到端：Testcontainers Redis + RedisCacheService set/get/delete/过期 |
| **rpcroute** | Redis Route 端到端：RedisRoutePublisher 写 Stream、RpcRedisRouteConsumerRunner 启停 |
| **performance** | 性能：insert/set/get 吞吐与 findById/get 延迟 |
| **stress** | 热点/压力：同 key 高并发、多 key 并发，验证无死锁、数据一致 |

## 运行

```bash
# 在项目根目录
mvn test -pl slg-framework-test
```

**环境要求**：本机需可运行 **Docker**（Testcontainers 会拉取 mysql:8.0、redis:7-alpine）。

- **Windows**：请先**启动 Docker Desktop**，并确认在终端执行 `docker version` 能正常返回（若一直无响应，说明守护进程未就绪）。本模块已配置：
  - `src/test/resources/testcontainers.properties`：`docker.client.strategy` + `docker.host=npipe:////./pipe/docker_engine`
  - `pom.xml` 的 `windows-docker` profile：为测试 JVM 传入 `DOCKER_HOST=npipe:////./pipe/docker_engine`
- 若仍报 `Could not find a valid Docker environment`：在 PowerShell 中先设置环境变量再执行测试：  
  `$env:DOCKER_HOST='npipe:////./pipe/docker_engine'; mvn test -pl slg-framework-test`

### 在 WSL2 中运行（推荐，可避免 Windows 命名管道 400 问题）

1. **打开 WSL2 终端**：从开始菜单启动 **Ubuntu**（或你安装的其它 WSL 发行版），不要用 `wsl` 从 PowerShell 里调单条命令（那样可能仍用 Windows 的 Maven/JDK）。
2. **进入项目目录**（二选一）：
   - 项目在 Windows 盘上：`cd /mnt/c/IDEAWorkspace/slgserver`（若没有 `/mnt/c`，先确认 WSL 是否挂载了 C 盘）。
   - 或把项目克隆到 WSL 家目录再 `cd ~/slgserver`。
3. **确保 WSL 里能用 Docker**：执行 `docker info`。若未安装，可启用 Docker Desktop 的 “Use the WSL 2 based engine” 和 “Integrate with my default WSL distro”，或在 WSL 内安装 Docker Engine。
4. **执行测试**：
   ```bash
   # 若 WSL 内未设 JAVA_HOME，且 JDK 在 Windows：可先
   export JAVA_HOME="/mnt/c/Program Files/Java/jdk-21"
   mvn test -pl slg-framework-test
   ```
   若 WSL 内已装 JDK/Maven，直接 `mvn test -pl slg-framework-test` 即可。

也可在项目根目录执行：`./slg-framework-test/run-integration-tests-wsl.sh`（需先 `chmod +x`）。

## 与单元测试的关系

- **单元测试**：在各框架模块内 `src/test/java`，Mock 外部依赖，快速回归。
- **本模块**：真实 MySQL/Redis，端到端 + 性能 + 压力，CI 中可选或按需执行。

测试结果可记录到 `.cursor/tests/results/`，参见 framework-test Skill。
