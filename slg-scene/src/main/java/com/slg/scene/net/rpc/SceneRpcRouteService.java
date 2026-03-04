package com.slg.scene.net.rpc;

import com.slg.net.rpc.route.IRouteSupportService;
import com.slg.net.socket.model.NetSession;
import com.slg.scene.SpringContext;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.core.config.SceneServerConfiguration;
import com.slg.scene.net.manager.InnerSessionManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 场景服务器 RPC 路由服务
 * 提供 RPC 调用的路由支持
 * 通过 rpc.client.route-service-class 配置项指定，由 RPC 框架自动实例化注册
 *
 * @author yangxunan
 * @date 2026/02/02
 */
public class SceneRpcRouteService implements IRouteSupportService {

    @Autowired
    private SceneServerConfiguration sceneServerConfiguration;

    @Override
    public NetSession getSessionByServerId(int serverId){
        return InnerSessionManager.getInstance().getSessionByServerId(serverId);
    }

    @Override
    public boolean isLocal(int serverId){
        return sceneServerConfiguration.getServerId() == serverId;
    }

    @Override
    public int getLocalServerId(){
        return sceneServerConfiguration.getServerId();
    }

    @Override
    public int getPlayerCurrentSceneServerId(long playerId){
        throw new UnsupportedOperationException("场景服不允许使用当前场景路由，想找玩家先回到Game");
    }

    @Override
    public int getPlayerMainSceneServerId(long playerId){
        throw new UnsupportedOperationException("场景服不允许使用主场景路由，想找玩家先回到Game");
    }

    @Override
    public int getPlayerGameServerId(long playerId){
        ScenePlayer scenePlayer = SpringContext.getScenePlayerManager().getScenePlayer(playerId);
        if (scenePlayer != null) {
            return scenePlayer.getGameServerId();
        }
        return 0;
    }
}
