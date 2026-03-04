#!/bin/bash
# ============================================================
# SLG 项目 - Zookeeper 初始化脚本
# 预设 GameServer 和 SceneServer 的配置节点
#
# 用法：
#   方式1（容器内执行）：
#     docker exec -it slg-zookeeper bash /init-zk.sh
#
#   方式2（本地 zkCli 执行）：
#     zkCli.sh -server localhost:2181 < init-zk-commands.txt
#
# 说明：
#   - 所有路径基于 base-path /slg（与 application.yml 中 zookeeper.base-path 一致）
#   - instance 节点是 EPHEMERAL 临时节点，由服务进程启动时自动创建，不在此预设
#   - 脚本幂等：重复执行不会覆盖已有数据（仅在节点不存在时创建）
# ============================================================

ZK_CLI="/apache-zookeeper-*/bin/zkCli.sh"
ZK_HOST="localhost:2181"

# 等待 Zookeeper 就绪
wait_for_zk() {
    echo "等待 Zookeeper 启动..."
    for i in $(seq 1 30); do
        if echo "ruok" | nc localhost 2181 2>/dev/null | grep -q "imok"; then
            echo "Zookeeper 已就绪"
            return 0
        fi
        sleep 1
    done
    echo "Zookeeper 启动超时"
    exit 1
}

# 创建持久化节点（如果不存在）
# 参数: $1=路径 $2=值
create_node() {
    local path="$1"
    local value="$2"
    $ZK_CLI -server $ZK_HOST create "$path" "$value" 2>/dev/null
}

# 创建节点（父节点必须已存在）
create_if_not_exists() {
    local path="$1"
    local value="$2"
    # zkCli create 在节点已存在时会报错但不影响脚本
    create_node "$path" "$value"
}

wait_for_zk

echo ""
echo "========================================="
echo " 开始初始化 SLG Zookeeper 配置"
echo "========================================="
echo ""

# ========================= 根节点 =========================

echo "[1/4] 创建根节点..."
create_if_not_exists "/slg" ""
create_if_not_exists "/slg/GameServers" ""
create_if_not_exists "/slg/SceneServers" ""

# ========================= GameServer 1 =========================

echo "[2/4] 创建 GameServer 1 配置..."

GS1="/slg/GameServers/1"
create_if_not_exists "$GS1" ""

# --- 网络配置 ---
create_if_not_exists "$GS1/game_ip"        "127.0.0.1"
create_if_not_exists "$GS1/game_host"       "localhost"
create_if_not_exists "$GS1/game_port"       "50001"

# --- RPC 配置 ---
create_if_not_exists "$GS1/rpc_ip"          "127.0.0.1"
create_if_not_exists "$GS1/rpc_port"        "51001"

# --- 服务器状态 ---
create_if_not_exists "$GS1/enable"          "true"
create_if_not_exists "$GS1/inServerList"    "true"
create_if_not_exists "$GS1/dbVersion"       "1"
create_if_not_exists "$GS1/timeZoneOffset"  "28800"

# --- 运营配置（开发阶段占位，当前不生效） ---
create_if_not_exists "$GS1/openTimeMs"          "0"
create_if_not_exists "$GS1/registedRole"        "0"
create_if_not_exists "$GS1/mergeServerVersion"  "0"
create_if_not_exists "$GS1/diversion_config"    "{}"
create_if_not_exists "$GS1/diversion_switch"    "close"
create_if_not_exists "$GS1/multiRoleServerShow" "false"

# --- Redis 子树 ---
create_if_not_exists "$GS1/Redis"           ""
create_if_not_exists "$GS1/Redis/host"      "localhost"
create_if_not_exists "$GS1/Redis/port"      "6379"
create_if_not_exists "$GS1/Redis/password"  ""

# --- MongoDB 子树 ---
create_if_not_exists "$GS1/MongoDB"         ""
create_if_not_exists "$GS1/MongoDB/db_name" "slg_game"
create_if_not_exists "$GS1/MongoDB/url"     "mongodb://localhost:27017/slg_game"

# --- 配置完成标记 ---
create_if_not_exists "$GS1/GAME_CONFIG_END_FLAG" "GAME_CONFIG_END_FLAG"

# ========================= SceneServer 2 =========================
# Scene 2 绑定 Game 1（独立部署模式：Game 和 Scene 分进程）

echo "[3/4] 创建 SceneServer 2 配置..."

SS2="/slg/SceneServers/2"
create_if_not_exists "$SS2" ""

# --- RPC 配置 ---
create_if_not_exists "$SS2/rpc_ip"          "127.0.0.1"
create_if_not_exists "$SS2/rpc_port"        "51002"

# --- 绑定关系 ---
create_if_not_exists "$SS2/bind_game_id"    "1"

# --- 服务器状态 ---
create_if_not_exists "$SS2/enable"          "true"
create_if_not_exists "$SS2/dbVersion"       "1"
create_if_not_exists "$SS2/timeZoneOffset"  "28800"

# --- Redis 子树 ---
create_if_not_exists "$SS2/Redis"           ""
create_if_not_exists "$SS2/Redis/host"      "localhost"
create_if_not_exists "$SS2/Redis/port"      "6379"
create_if_not_exists "$SS2/Redis/password"  ""

# --- MongoDB 子树 ---
create_if_not_exists "$SS2/MongoDB"         ""
create_if_not_exists "$SS2/MongoDB/db_name" "slg_scene"
create_if_not_exists "$SS2/MongoDB/url"     "mongodb://localhost:27017/slg_scene"

# --- 配置完成标记 ---
create_if_not_exists "$SS2/SCENE_CONFIG_END_FLAG" "SCENE_CONFIG_END_FLAG"

# ========================= SingleStart 模式预设 =========================
# SingleStart 使用 GameServer 1 + SceneServer 1（同一进程，共享 ID=1）
# 如需 SingleStart 模式，需要额外创建 SceneServer 1 的配置

echo "[4/4] 创建 SceneServer 1 配置（SingleStart 模式用）..."

SS1="/slg/SceneServers/1"
create_if_not_exists "$SS1" ""

# --- RPC 配置（SingleStart 模式下 RPC 共用 51001） ---
create_if_not_exists "$SS1/rpc_ip"          "127.0.0.1"
create_if_not_exists "$SS1/rpc_port"        "51001"

# --- 绑定关系（绑定自身 GameServer 1） ---
create_if_not_exists "$SS1/bind_game_id"    "1"

# --- 服务器状态 ---
create_if_not_exists "$SS1/enable"          "true"
create_if_not_exists "$SS1/dbVersion"       "1"
create_if_not_exists "$SS1/timeZoneOffset"  "28800"

# --- Redis 子树 ---
create_if_not_exists "$SS1/Redis"           ""
create_if_not_exists "$SS1/Redis/host"      "localhost"
create_if_not_exists "$SS1/Redis/port"      "6379"
create_if_not_exists "$SS1/Redis/password"  ""

# --- MongoDB 子树 ---
create_if_not_exists "$SS1/MongoDB"         ""
create_if_not_exists "$SS1/MongoDB/db_name" "slg_singlestart"
create_if_not_exists "$SS1/MongoDB/url"     "mongodb://localhost:27017/slg_singlestart"

# --- 配置完成标记 ---
create_if_not_exists "$SS1/SCENE_CONFIG_END_FLAG" "SCENE_CONFIG_END_FLAG"

echo ""
echo "========================================="
echo " Zookeeper 初始化完成！"
echo "========================================="
echo ""
echo "已创建的节点树："
echo ""
echo "/slg"
echo "├── /GameServers"
echo "│   └── /1 (GameServer - 客户端端口 50001, RPC 端口 51001)"
echo "├── /SceneServers"
echo "│   ├── /1 (SceneServer - SingleStart 模式, RPC 端口 51001)"
echo "│   └── /2 (SceneServer - 独立模式, RPC 端口 51002, 绑定 Game 1)"
echo ""
echo "注意："
echo "  - instance 临时节点由服务进程启动时自动创建"
echo "  - 独立部署模式使用 GameServer 1 + SceneServer 2"
echo "  - SingleStart 模式使用 GameServer 1 + SceneServer 1"
echo ""
