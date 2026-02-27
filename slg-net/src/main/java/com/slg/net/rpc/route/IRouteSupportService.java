package com.slg.net.rpc.route;

import com.slg.net.socket.model.NetSession;

/**
 * rpc路由支持 需要在业务模块中实现，接入模块内部的session管理接口
 *
 * @author yangxunan
 * @date 2026/1/23
 */
public interface IRouteSupportService {

    NetSession getSessionByServerId(int serverId);

    boolean isLocal(int serverId);

    int getLocalServerId();

    int getPlayerCurrentSceneServerId(long playerId);

    int getPlayerMainSceneServerId(long playerId);

    int getPlayerGameServerId(long playerId);

}
