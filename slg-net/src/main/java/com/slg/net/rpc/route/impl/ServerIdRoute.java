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
 * 基于 ServerId 的路由策略
 * 根据目标服务器 ID 路由 RPC 调用
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class ServerIdRoute extends AbstractRpcRoute {

    /**
     * 路由参数：需要一个 int 类型的 serverId
     */
    @Getter
    private final Class<?>[] routeParams = new Class<?>[]{int.class};

    @Override
    public void sendMsg(IM_RpcRequest request, RpcMethodMeta rpcMethodMeta, Object... params) {
        if (params == null || params.length == 0) {
            LoggerUtil.error("[RPC] 路由参数不足");
            throw new RpcException("路由参数不足");
        }
        
        // params 已经是提取出来的路由参数，直接使用第一个参数
        int serverId = (int) params[0];
        NetSession netSession = routeSupportService.getSessionByServerId(serverId);
        
        // RPC 功能单一化，不负责连接的创建
        if (netSession == null || !netSession.isActive()) {
            LoggerUtil.error("[RPC] 未找到与服务器 {} 的有效连接", serverId);
            throw new RpcException("未找到与服务器的有效连接: " + serverId);
        }

        // 发送消息
        netSession.sendMessage(request);
        LoggerUtil.debug("[RPC] 发送消息到服务器: serverId={}, method={}", 
                serverId, request.getMethodMarker());
    }

    @Override
    public int resolveTargetServerId(RpcMethodMeta rpcMethodMeta, Object... routeParams) {
        if (routeParams == null || routeParams.length == 0) {
            return 0;
        }
        return (int) routeParams[0];
    }

    @Override
    public boolean isLocal(RpcMethodMeta rpcMethodMeta, Object... params) {
        if (rpcMethodMeta.getRouteParamsIndex() == null || rpcMethodMeta.getRouteParamsIndex().length == 0) {
            return false;
        }
        
        // 获取 serverId 参数
        int routeParamIndex = rpcMethodMeta.getRouteParamsIndex()[0];
        int serverId = (int) params[routeParamIndex];
        
        // 判断是否是本地服务器
        return routeSupportService.isLocal(serverId);
    }

}
