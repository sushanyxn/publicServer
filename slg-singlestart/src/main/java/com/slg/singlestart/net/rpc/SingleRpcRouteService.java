package com.slg.singlestart.net.rpc;

import com.slg.game.SpringContext;
import com.slg.game.base.player.model.Player;
import com.slg.game.core.config.GameServerConfiguration;
import com.slg.game.net.manager.InnerSessionManager;
import com.slg.net.rpc.route.IRpcRouteSupportService;
import com.slg.net.rpc.route.IRouteSupportService;
import com.slg.net.socket.model.NetSession;
import com.slg.scene.base.model.ScenePlayer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yangxunan
 * @date 2026/2/24
 */
public class SingleRpcRouteService implements IRouteSupportService, IRpcRouteSupportService {

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
        ScenePlayer scenePlayer = com.slg.scene.SpringContext.getScenePlayerManager().getScenePlayer(playerId);
        if (scenePlayer != null) {
            return scenePlayer.getGameServerId();
        }
        return 0;
    }
}
