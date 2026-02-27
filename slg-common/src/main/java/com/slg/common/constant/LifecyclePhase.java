package com.slg.common.constant;

/**
 * SmartLifecycle 生命周期阶段常量接口
 * 用于控制 Spring Bean 的启动和关闭顺序
 * 
 * Phase 值规则：
 * - 值越小，start() 越早执行，stop() 越晚执行
 * - 值越大，start() 越晚执行，stop() 越早执行
 * 
 * @author yangxunan
 * @date 2026/01/22
 */
public interface LifecyclePhase {

    /**
     * 配置表检查：最早启动，最晚关闭
     * 确保配置数据完整性，为后续所有服务提供基础
     */
    int TABLE_CHECK = Integer.MAX_VALUE - 7000;

    /**
     * Zookeeper：在配置表检查之后、数据库之前
     * 用于读取远程配置，为后续数据库等服务提供配置支持
     */
    int ZOOKEEPER = Integer.MAX_VALUE - 6500;

    /**
     * 数据库层：在配置表检查之后
     * 包括数据库连接验证、连接池初始化、缓存刷盘等
     */
    int DATABASE = Integer.MAX_VALUE - 6000;

    /**
     * Redis：在数据库之后
     * 用于缓存服务和排行榜等
     */
    int REDIS = Integer.MAX_VALUE - 5900;

    /**
     * 数据加载层：在数据库之后
     * 包括玩家数据加载、缓存预热等
     */
    int DATA_LOADING = Integer.MAX_VALUE - 5000;

    /**
     * game业务初始化
     */
    int GAME_INIT = Integer.MAX_VALUE - 4000;

    /**
     * scene业务初始化
     */
    int SCENE_INIT = Integer.MAX_VALUE - 3000;

    /**
     * 合并进程业务初始化
     */
    int SINGLE_INIT = Integer.MAX_VALUE - 2900;

    /**
     * tick业务初始化
     */
    int TICK_INIT = Integer.MAX_VALUE - 2000;

    /**
     * Web 服业务初始化：在 DATABASE/REDIS/ZOOKEEPER 之后、RPC_SERVER 之前
     * 仅 slg-web 导量服进程使用
     */
    int WEB_INIT = Integer.MAX_VALUE - 3500;

    /**
     * 日志服业务初始化：在 DATABASE 之后
     * 仅 slg-log 日志分析服进程使用
     */
    int LOG_INIT = Integer.MAX_VALUE - 3400;

    /**
     * RPC 服务器：在业务数据准备好之后启动
     */
    int RPC_SERVER = Integer.MAX_VALUE - 1000;

    /**
     * Thrift 协议适配层：在 WebSocket 服务器之前启动
     * 为 Thrift 客户端提供协议转换服务
     */
    int THRIFT_ADAPTER = Integer.MAX_VALUE - 100;

    /**
     * WebSocket 服务器：最晚启动，最早关闭
     * 确保所有后端服务都准备就绪
     */
    int WEBSOCKET_SERVER = Integer.MAX_VALUE;
}

