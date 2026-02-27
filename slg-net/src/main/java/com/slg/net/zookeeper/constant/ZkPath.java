package com.slg.net.zookeeper.constant;

/**
 * Zookeeper 节点路径常量
 * 定义 GameServers 和 SceneServers 下所有节点的路径名称
 *
 * <p>节点存储规则：每个配置项是独立的子节点，值存储为节点 data（非 JSON 整体）
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public final class ZkPath {

    private ZkPath() {
    }

    // ========================= 一级路径 =========================

    public static final String GAME_SERVERS = "/GameServers";
    public static final String SCENE_SERVERS = "/SceneServers";

    // ========================= 实例节点（EPHEMERAL） =========================

    /** 进程存活标记，连接时创建，断开时自动销毁 */
    public static final String INSTANCE = "instance";

    // ========================= GameServer 子节点 =========================

    /** 对客户端的外部IP */
    public static final String GAME_IP = "game_ip";
    /** 主机名/域名 */
    public static final String GAME_HOST = "game_host";
    /** 对客户端的外部端口 */
    public static final String GAME_PORT = "game_port";

    // ========================= 共用 RPC 子节点 =========================

    /** RPC 对外IP */
    public static final String RPC_IP = "rpc_ip";
    /** RPC 端口 */
    public static final String RPC_PORT = "rpc_port";

    // ========================= 共用状态子节点 =========================

    /** 是否启用 */
    public static final String ENABLE = "enable";
    /** 数据库版本号 */
    public static final String DB_VERSION = "dbVersion";
    /** 时区偏移（秒） */
    public static final String TIME_ZONE_OFFSET = "timeZoneOffset";

    // ========================= GameServer 专有子节点 =========================

    /** 是否出现在服务器列表 */
    public static final String IN_SERVER_LIST = "inServerList";
    /** 开服时间戳（毫秒） */
    public static final String OPEN_TIME_MS = "openTimeMs";
    /** 已注册角色数 */
    public static final String REGISTED_ROLE = "registedRole";
    /** 合服版本号 */
    public static final String MERGE_SERVER_VERSION = "mergeServerVersion";
    /** 导量配置（JSON） */
    public static final String DIVERSION_CONFIG = "diversion_config";
    /** 导量开关：close/open/auto */
    public static final String DIVERSION_SWITCH = "diversion_switch";
    /** 是否多角色服显示 */
    public static final String MULTI_ROLE_SERVER_SHOW = "multiRoleServerShow";
    /** GameServer 配置完成标记 */
    public static final String GAME_CONFIG_END_FLAG = "GAME_CONFIG_END_FLAG";

    // ========================= SceneServer 专有子节点 =========================

    /** 绑定的 GameServer ID */
    public static final String BIND_GAME_ID = "bind_game_id";
    /** SceneServer 配置完成标记 */
    public static final String SCENE_CONFIG_END_FLAG = "SCENE_CONFIG_END_FLAG";

    // ========================= Redis 子树 =========================

    public static final String REDIS = "Redis";
    public static final String REDIS_HOST = "host";
    public static final String REDIS_PORT = "port";
    public static final String REDIS_PASSWORD = "password";

    // ========================= MongoDB 子树 =========================

    public static final String MONGO = "MongoDB";
    public static final String MONGO_DB_NAME = "db_name";
    public static final String MONGO_URL = "url";

    // ========================= 工具方法 =========================

    /**
     * 获取 GameServer 根路径
     *
     * @param serverId 服务器ID
     * @return 路径，如 /GameServers/1
     */
    public static String gameServerPath(int serverId) {
        return GAME_SERVERS + "/" + serverId;
    }

    /**
     * 获取 SceneServer 根路径
     *
     * @param serverId 服务器ID
     * @return 路径，如 /SceneServers/2
     */
    public static String sceneServerPath(int serverId) {
        return SCENE_SERVERS + "/" + serverId;
    }

    /**
     * 获取服务器下的 instance 临时节点路径
     *
     * @param serverBasePath 服务器根路径（如 /GameServers/1）
     * @return instance 节点路径
     */
    public static String instancePath(String serverBasePath) {
        return serverBasePath + "/" + INSTANCE;
    }

    /**
     * 获取服务器下某个子节点路径
     *
     * @param serverBasePath 服务器根路径
     * @param childNode      子节点名称
     * @return 完整子节点路径
     */
    public static String childPath(String serverBasePath, String childNode) {
        return serverBasePath + "/" + childNode;
    }

    /**
     * 获取 Redis 子树下某个字段路径
     *
     * @param serverBasePath 服务器根路径
     * @param field          Redis 字段名（如 host、port、password）
     * @return 完整路径
     */
    public static String redisFieldPath(String serverBasePath, String field) {
        return serverBasePath + "/" + REDIS + "/" + field;
    }

    /**
     * 获取 MongoDB 子树下某个字段路径
     *
     * @param serverBasePath 服务器根路径
     * @param field          MongoDB 字段名（如 db_name、url）
     * @return 完整路径
     */
    public static String mongoFieldPath(String serverBasePath, String field) {
        return serverBasePath + "/" + MONGO + "/" + field;
    }
}
