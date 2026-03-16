package com.slg.net.rpc.route.impl;

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
 * <p><b>传输可靠性策略</b>（由 {@link RedisRoutePublisher} 的 Pipeline 机制实现）：
 * <ul>
 *   <li><b>有返回值（非 void）的 RPC 方法</b>：走可靠确认模式。Pipeline 批量 XADD 后等待
 *       Redis 返回所有 RecordId，确保消息写入 Stream。若 XADD 失败则回调 Future 会因超时异常。</li>
 *   <li><b>void RPC 方法</b>：走 fire-and-forget 模式。Pipeline 批量 XADD 后不等待确认，
 *       发送方无法感知写入是否成功。这是安全的，因为 void 方法的 callBackId = 0，
 *       发送方不注册回调也不持有 Future，即使消息丢失也不会产生悬挂状态。</li>
 *   <li><b>适用前提</b>：void 方法的业务语义应当是"尽力而为"的通知或触发，而非关键状态变更。
 *       如果某个 void 方法需要保证投递可靠性（如跨服扣资源），应将返回值改为
 *       {@code CompletableFuture<Void>} 以强制走可靠确认路径。</li>
 * </ul>
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

        boolean isVoid = rpcMethodMeta.getMethod().getReturnType().equals(Void.TYPE);
        if (isVoid) {
            redisRoutePublisher.publishFireAndForget(serverId, request);
        } else {
            redisRoutePublisher.publish(serverId, request);
        }
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
