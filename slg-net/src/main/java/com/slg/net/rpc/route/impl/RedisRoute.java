package com.slg.net.rpc.route.impl;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.route.AbstractRpcRoute;
import com.slg.net.rpc.route.redis.RedisRoutePublisher;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 基于 Redis Stream 的 RPC 路由策略
 * 通过独立转发 Redis 实现跨 Game 服的 RPC 调用，无需两两建立 WebSocket 连接
 *
 * <p>路由参数：与 {@link ServerIdRoute} 一致，需要一个 int 类型的 serverId
 *
 * @author yangxunan
 * @date 2026/03/04
 */
public class RedisRoute extends AbstractRpcRoute {

    private static final Class<?>[] ROUTE_PARAMS = new Class<?>[]{int.class};

    @Autowired
    private RedisRoutePublisher redisRoutePublisher;

    @Override
    public Class<?>[] getRouteParams() {
        return ROUTE_PARAMS;
    }

    @Override
    public void sendMsg(IM_RpcRequest request, RpcMethodMeta rpcMethodMeta, Object... params) {
        if (params == null || params.length == 0) {
            throw new IllegalArgumentException("[RpcRoute] RedisRoute 路由参数不足");
        }
        int serverId = (int) params[0];
        redisRoutePublisher.publish(serverId, request);
    }

    @Override
    public boolean isLocal(RpcMethodMeta rpcMethodMeta, Object... params) {
        if (rpcMethodMeta.getRouteParamsIndex() == null || rpcMethodMeta.getRouteParamsIndex().length == 0) {
            return false;
        }
        int serverId = (int) params[rpcMethodMeta.getRouteParamsIndex()[0]];
        return routeSupportService.isLocal(serverId);
    }

    @Override
    public int resolveTargetServerId(RpcMethodMeta rpcMethodMeta, Object... routeParams) {
        if (routeParams == null || routeParams.length == 0) {
            return 0;
        }
        return (int) routeParams[0];
    }
}
