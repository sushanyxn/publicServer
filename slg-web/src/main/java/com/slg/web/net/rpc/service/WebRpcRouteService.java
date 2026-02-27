package com.slg.web.net.rpc.service;

import com.slg.net.rpc.route.IRouteSupportService;
import com.slg.net.socket.model.NetSession;
import com.slg.web.net.rpc.manager.WebInnerSessionManager;

/**
 * Web 服 RPC 路由实现
 * Web 服是被连接方，不管理玩家，路由逻辑较简单：
 * - getSessionByServerId：通过 serverId 获取已连接的 game 服会话
 * - 玩家相关路由方法返回 0（Web 不管理玩家）
 * 通过 rpc.client.route-service-class 配置项指定，由 RPC 框架自动实例化注册
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class WebRpcRouteService implements IRouteSupportService {

    @Override
    public NetSession getSessionByServerId(int serverId) {
        return WebInnerSessionManager.getInstance().getSessionByServerId(serverId);
    }

    @Override
    public boolean isLocal(int serverId) {
        return false;
    }

    @Override
    public int getLocalServerId() {
        return 0;
    }

    @Override
    public int getPlayerCurrentSceneServerId(long playerId) {
        return 0;
    }

    @Override
    public int getPlayerMainSceneServerId(long playerId) {
        return 0;
    }

    @Override
    public int getPlayerGameServerId(long playerId) {
        return 0;
    }
}
