package com.slg.frameworktest.rpcroute;

import com.slg.net.rpc.route.IRpcRouteSupportService;
import com.slg.net.rpc.route.IRouteSupportService;
import com.slg.net.socket.model.NetSession;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 集成测试用路由支持服务实现，同时满足 IRouteSupportService 和 IRpcRouteSupportService
 * 固定本服 ID 为 1，其余方法返回测试默认值
 *
 * @author framework-test
 */
@Component
@Primary
public class TestRpcRouteSupportService implements IRpcRouteSupportService, IRouteSupportService {

    private static final int LOCAL_SERVER_ID = 1;

    @Override
    public int getLocalServerId() {
        return LOCAL_SERVER_ID;
    }

    @Override
    public NetSession getSessionByServerId(int serverId) {
        return null;
    }

    @Override
    public boolean isLocal(int serverId) {
        return serverId == LOCAL_SERVER_ID;
    }

    @Override
    public int getPlayerCurrentSceneServerId(long playerId) {
        return LOCAL_SERVER_ID;
    }

    @Override
    public int getPlayerMainSceneServerId(long playerId) {
        return LOCAL_SERVER_ID;
    }

    @Override
    public int getPlayerGameServerId(long playerId) {
        return LOCAL_SERVER_ID;
    }
}
