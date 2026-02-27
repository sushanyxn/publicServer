package com.slg.net.rpc.route.impl;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.rpc.exception.RpcException;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.route.AbstractRpcRoute;
import com.slg.net.socket.model.NetSession;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/1/27
 */
@Component
public class PlayerMainSceneRoute extends AbstractRpcRoute {
    /**
     * 路由参数：需要一个 long 类型的 playerId
     */
    @Getter
    private final Class<?>[] routeParams = new Class<?>[]{long.class};

    @Override
    public void sendMsg(IM_RpcRequest request, RpcMethodMeta rpcMethodMeta, Object... params){
        if (params == null || params.length == 0) {
            LoggerUtil.error("[RPC] 路由参数不足");
            throw new RpcException("路由参数不足");
        }

        long playerId = (long) params[0];
        int mainServerId = routeSupportService.getPlayerMainSceneServerId(playerId);
        NetSession netSession = routeSupportService.getSessionByServerId(mainServerId);

        // RPC 功能单一化，不负责连接的创建
        if (netSession == null || !netSession.isActive()) {
            LoggerUtil.error("[RPC] 未找到与服务器 {} 的有效连接", mainServerId);
            throw new RpcException("未找到与服务器的有效连接: " + mainServerId);
        }

        // 发送消息
        netSession.sendMessage(request);
    }

    @Override
    public int resolveTargetServerId(RpcMethodMeta rpcMethodMeta, Object... routeParams) {
        if (routeParams == null || routeParams.length == 0) {
            return 0;
        }
        long playerId = (long) routeParams[0];
        return routeSupportService.getPlayerMainSceneServerId(playerId);
    }

    @Override
    public boolean isLocal(RpcMethodMeta rpcMethodMeta, Object... params){
        if (rpcMethodMeta.getRouteParamsIndex() == null || rpcMethodMeta.getRouteParamsIndex().length == 0) {
            return false;
        }

        // 获取 serverId 参数
        int routeParamIndex = rpcMethodMeta.getRouteParamsIndex()[0];
        long playerId = (long) params[routeParamIndex];
        int mainServerId = routeSupportService.getPlayerMainSceneServerId(playerId);

        // 判断是否是本地服务器
        return routeSupportService.isLocal(mainServerId);
    }
}
