package com.slg.game.scene.facade;

import com.slg.game.base.player.model.Player;
import com.slg.game.scene.service.GameSceneService;
import com.slg.net.message.clientmessage.scene.packet.CM_EnterScene;
import com.slg.net.message.clientmessage.scene.packet.CM_LoadSceneFinish;
import com.slg.net.message.clientmessage.scene.packet.CM_Watch;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/2/2
 */
@Component
public class SceneFacade {

    @Autowired
    private GameSceneService gameSceneService;

    @MessageHandler
    public void reqEnterScene(NetSession session, CM_EnterScene enterScene, Player player) {
        gameSceneService.reqEnterScene(player, enterScene.getServerId(), enterScene.getSceneId());
    }

    @MessageHandler
    public void loadSceneFinish(NetSession session, CM_LoadSceneFinish enterScene, Player player) {
        gameSceneService.loadSceneFinish(player);
    }

    @MessageHandler
    public void watch(NetSession session, CM_Watch watch, Player player) {
        gameSceneService.watchScene(player, watch.getX(), watch.getY(), watch.getLayer());
    }
}
