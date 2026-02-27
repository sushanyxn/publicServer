package com.slg.game.scene.service;

import com.slg.game.base.player.model.Player;
import com.slg.game.base.player.model.SceneContext;
import com.slg.game.scene.handler.AbstractSceneHandler;
import com.slg.game.scene.manager.GameSceneManager;
import com.slg.game.scene.table.SceneTable;
import com.slg.net.rpc.anno.RpcRef;
import com.slg.net.rpc.impl.scene.ISceneOptionRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * game进程的scene服务 接收客户端协议并转发
 *
 * @author yangxunan
 * @date 2026/1/27
 */
@Component
public class GameSceneService {

    @RpcRef
    private ISceneOptionRpcService sceneService;

    @Autowired
    private GameSceneManager sceneManager;

    public void reqEnterScene(Player player, int serverId, int sceneId){

        SceneTable sceneTable = sceneManager.getSceneTable(sceneId);
        if (sceneTable == null) {
            return;
        }
        AbstractSceneHandler sceneHandler = AbstractSceneHandler.getHandler(sceneTable.getType());
        if (sceneHandler == null) {
            return;
        }
        sceneHandler.verifyEnter(player, serverId, sceneId);

    }

    public void loadSceneFinish(Player player){
        SceneContext sceneContext = player.getSceneContext();
        if (sceneContext.getGoingSceneId() <= 0) {
            // 没有接收到切图协议
            return;
        }
        SceneTable sceneTable = sceneManager.getSceneTable(sceneContext.getCurrentSceneId());
        AbstractSceneHandler sceneHandler = AbstractSceneHandler.getHandler(sceneTable.getType());

        int sceneId = sceneContext.getGoingSceneId();
        int serverId = sceneContext.getGoingSceneServerId();
        // 清理切图缓存，防止重复触发
        sceneContext.clearGoingCache();
        sceneHandler.enterScene(player, sceneId, serverId);
    }

    public void watchScene(Player player, int x, int y, int layer){
        sceneService.watch(player.getId(), x, y, layer);
    }

}
