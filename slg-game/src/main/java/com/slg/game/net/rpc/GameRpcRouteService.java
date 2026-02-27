package com.slg.game.net.rpc;

import com.slg.game.SpringContext;
import com.slg.game.base.player.model.Player;
import com.slg.game.core.config.GameServerConfiguration;
import com.slg.game.net.manager.InnerSessionManager;
import com.slg.net.rpc.route.IRouteSupportService;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * game rpc路由接入
 * 通过 rpc.client.route-service-class 配置项指定，由 RPC 框架自动实例化注册
 *
 * @author yangxunan
 * @date 2026/1/26
 */
public class GameRpcRouteService implements IRouteSupportService {

    @Autowired
    private GameServerConfiguration gameServerConfiguration;

    @Override
    public NetSession getSessionByServerId(int serverId){
        return InnerSessionManager.getInstance().getSessionByServerId(serverId);
    }

    @Override
    public boolean isLocal(int serverId){
        return gameServerConfiguration.getServerId() == serverId;
    }

    @Override
    public int getLocalServerId(){
        return gameServerConfiguration.getServerId();
    }

    @Override
    public int getPlayerCurrentSceneServerId(long playerId){
        Player player = SpringContext.getPlayerManager().getPlayer(playerId);
        if (player != null){
            // 当前场景存在上下文中
            return player.getSceneContext().getCurrentSceneServerId();
        }
        return 0;
    }

    @Override
    public int getPlayerMainSceneServerId(long playerId){
        Player player = SpringContext.getPlayerManager().getPlayer(playerId);
        if (player != null){
            // 主场景存储在entity中 需要入库
            return player.getPlayerEntity().getSceneServerId();
        }
        return 0;
    }

    @Override
    public int getPlayerGameServerId(long playerId){
        Player player = SpringContext.getPlayerManager().getPlayer(playerId);
        if (player != null){
            // 就是本服的玩家
            return gameServerConfiguration.getServerId();
        }
        // 非本服玩家，需要借助redis实现信息查询
        return 0;
    }
}
