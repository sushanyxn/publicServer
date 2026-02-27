package com.slg.game.scene.handler;

import com.slg.game.base.player.model.Player;
import com.slg.game.base.player.model.SceneContext;
import com.slg.game.net.ToClientPacketUtil;
import com.slg.game.scene.manager.GameSceneManager;
import com.slg.game.scene.table.SceneTable;
import com.slg.net.message.clientmessage.scene.packet.SM_EnterScene;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 虚拟场景，实际上不需要场景实体，但服务端需要能够感知客户端的场景切换
 * 切换场景也需要走相同的流程，避免切场景逻辑的混乱
 *
 * @author yangxunan
 * @date 2026/2/2
 */
public abstract class EmptySceneHandler extends AbstractSceneHandler{

    @Autowired
    protected GameSceneManager sceneManager;

    @Override
    public void verifyEnter(Player player, int sceneServerId, int sceneId){
        // 虚拟场景不设置进入条件

        // 退出现在的场景
        SceneContext sceneContext = player.getSceneContext();
        if (sceneContext.getCurrentSceneId() > 0){
            SceneTable sceneTable = sceneManager.getSceneTable(sceneContext.getCurrentSceneId());
            AbstractSceneHandler handler = getHandler(sceneTable.getType());
            handler.exitScene(player, sceneContext.getCurrentSceneServerId(), sceneContext.getCurrentSceneId());
        }

        sceneContext.updateGoingScene(sceneServerId, sceneId);
        // 通知客户端进入
        ToClientPacketUtil.send(player, SM_EnterScene.valueOf(0));
    }

    @Override
    public void enterScene(Player player, int sceneServerId, int sceneId){
        // 虚拟场景没有场景实体

        // 更新场景信息
        player.getSceneContext().updateScene(sceneServerId, sceneId);
    }

    @Override
    public void exitScene(Player player, int sceneServerId, int sceneId){
        // 退出时也不需要额外处理
    }
}
