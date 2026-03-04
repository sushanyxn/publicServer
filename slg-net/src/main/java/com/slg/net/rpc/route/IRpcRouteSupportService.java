package com.slg.net.rpc.route;

/**
 * RPC Redis 路由专用接口
 * 定义 Redis Stream 路由所需的 channel（Stream key）查询方法
 *
 * <p>与 {@link IRouteSupportService} 并列、互不依赖。
 * 需要使用 Redis 跨服转发的进程（如 slg-game）的路由服务实例应同时实现两个接口。
 * RedisRoute、RedisRoutePublisher、RpcRedisRouteConsumerRunner 只依赖本接口。
 *
 * @author yangxunan
 * @date 2026/03/04
 */
public interface IRpcRouteSupportService {

    /**
     * 获取目标服务器的 Redis Stream key（请求 channel）
     *
     * @param serverId 目标服务器ID
     * @return Redis Stream key，默认格式 "rpc:route:{serverId}"
     */
    default String getRedisRouteChannel(int serverId) {
        return "rpc:route:" + serverId;
    }

    /**
     * 获取本服务器的 Redis Stream key（请求 channel）
     *
     * @return 本服请求 Stream key，默认格式 "rpc:route:{localServerId}"
     */
    default String getLocalRedisRouteChannel() {
        return "rpc:route:" + getLocalServerId();
    }

    /**
     * 获取本服务器的 Redis Stream key（响应 channel）
     *
     * @return 本服响应 Stream key，默认格式 "rpc:route:resp:{localServerId}"
     */
    default String getLocalRedisRespChannel() {
        return "rpc:route:resp:" + getLocalServerId();
    }

    /**
     * 获取本服务器ID
     *
     * @return 本服务器ID
     */
    int getLocalServerId();
}
