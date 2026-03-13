package com.slg.net.zookeeper.model;

import com.slg.net.zookeeper.constant.ZkPath;
import lombok.Getter;
import lombok.Setter;

/**
 * SceneServer 在 ZK 中的完整注册信息
 * 对应节点树：/SceneServers/{serverId}/
 *
 * <p>Scene 服务器没有客户端 WebSocket 入口，不涉及服务器列表等概念
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Getter
@Setter
public class SceneServerZkInfo {

    /** 服务器ID */
    private int serverId;

    // ========================= RPC 配置 =========================

    /** RPC 对外IP */
    private String rpcIp;

    /** RPC 端口 */
    private int rpcPort;

    // ========================= 绑定关系 =========================

    /** 绑定的 GameServer ID */
    private int bindGameId;

    // ========================= 状态信息 =========================

    /** 是否启用 */
    private boolean enable;

    /** 数据库版本号 */
    private String dbVersion;

    /** 时区偏移（秒） */
    private int timeZoneOffset;

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
            case ZkPath.RPC_IP -> this.rpcIp = value;
            case ZkPath.RPC_PORT -> this.rpcPort = parseIntSafe(value, 0);
            case ZkPath.BIND_GAME_ID -> this.bindGameId = parseIntSafe(value, 0);
            case ZkPath.ENABLE -> this.enable = Boolean.parseBoolean(value);
            case ZkPath.DB_VERSION -> this.dbVersion = value;
            case ZkPath.TIME_ZONE_OFFSET -> this.timeZoneOffset = parseIntSafe(value, 0);
            case ZkPath.INSTANCE -> this.alive = (value != null);
            case ZkPath.SCENE_CONFIG_END_FLAG -> { /* 忽略标记节点 */ }
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
}
