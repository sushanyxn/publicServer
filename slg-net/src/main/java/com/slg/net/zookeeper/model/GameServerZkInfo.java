package com.slg.net.zookeeper.model;

import com.slg.net.zookeeper.constant.ZkPath;
import lombok.Getter;
import lombok.Setter;

/**
 * GameServer 在 ZK 中的完整注册信息
 * 对应节点树：/GameServers/{serverId}/
 *
 * <p>每个字段对应一个独立的 ZK 子节点，Redis 和 MongoDB 为二级子树
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Getter
@Setter
public class GameServerZkInfo {

    /** 服务器ID */
    private int serverId;

    // ========================= 网络配置 =========================

    /** 对客户端的外部IP */
    private String gameIp;

    /** 主机名/域名 */
    private String gameHost;

    /** 对客户端的外部端口 */
    private int gamePort;

    // ========================= RPC 配置 =========================

    /** RPC 对外IP */
    private String rpcIp;

    /** RPC 端口 */
    private int rpcPort;

    // ========================= 状态信息 =========================

    /** 是否启用 */
    private boolean enable;

    /** 是否出现在服务器列表 */
    private boolean inServerList;

    /** 开服时间戳（毫秒） */
    private long openTimeMs;

    /** 已注册角色数 */
    private long registedRole;

    // ========================= 版本与时区 =========================

    /** 数据库版本号 */
    private String dbVersion;

    /** 时区偏移（秒） */
    private int timeZoneOffset;

    /** 合服版本号 */
    private int mergeServerVersion;

    // ========================= 导量配置 =========================

    /** 导量配置（JSON 字符串） */
    private String diversionConfig;

    /** 导量开关：close/open/auto */
    private String diversionSwitch;

    /** 是否多角色服显示 */
    private boolean multiRoleServerShow;

    // ========================= 数据库连接 =========================

    /** Redis 连接信息 */
    private RedisZkInfo redis;

    /** MongoDB 连接信息 */
    private MongoZkInfo mongo;

    // ========================= 运行状态 =========================

    /** 进程是否存活（通过 instance 临时节点判断，非 ZK 持久化字段） */
    private boolean alive;

    // ========================= 增量更新 =========================

    /**
     * 根据 ZK 子节点名称增量更新对应字段
     * 用于监听回调时避免全量重读
     *
     * @param field ZK 子节点名称（ZkPath 常量）
     * @param value 新值字符串
     * @return true 匹配到了字段并更新成功，false 表示未匹配（如 Redis/Mongo 子树）
     */
    public boolean updateField(String field, String value) {
        switch (field) {
            case ZkPath.GAME_IP -> this.gameIp = value;
            case ZkPath.GAME_HOST -> this.gameHost = value;
            case ZkPath.GAME_PORT -> this.gamePort = parseIntSafe(value, 0);
            case ZkPath.RPC_IP -> this.rpcIp = value;
            case ZkPath.RPC_PORT -> this.rpcPort = parseIntSafe(value, 0);
            case ZkPath.ENABLE -> this.enable = Boolean.parseBoolean(value);
            case ZkPath.IN_SERVER_LIST -> this.inServerList = Boolean.parseBoolean(value);
            case ZkPath.OPEN_TIME_MS -> this.openTimeMs = parseLongSafe(value, 0L);
            case ZkPath.REGISTED_ROLE -> this.registedRole = parseLongSafe(value, 0L);
            case ZkPath.DB_VERSION -> this.dbVersion = value;
            case ZkPath.TIME_ZONE_OFFSET -> this.timeZoneOffset = parseIntSafe(value, 0);
            case ZkPath.MERGE_SERVER_VERSION -> this.mergeServerVersion = parseIntSafe(value, 0);
            case ZkPath.DIVERSION_CONFIG -> this.diversionConfig = value;
            case ZkPath.DIVERSION_SWITCH -> this.diversionSwitch = value;
            case ZkPath.MULTI_ROLE_SERVER_SHOW -> this.multiRoleServerShow = Boolean.parseBoolean(value);
            case ZkPath.INSTANCE -> this.alive = (value != null);
            case ZkPath.GAME_CONFIG_END_FLAG -> { /* 忽略标记节点 */ }
            default -> { return false; }
        }
        return true;
    }

    private static int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long parseLongSafe(String value, long defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
