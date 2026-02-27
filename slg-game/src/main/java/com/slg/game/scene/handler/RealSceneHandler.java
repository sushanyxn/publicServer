package com.slg.game.scene.handler;

import com.slg.common.log.LoggerUtil;
import com.slg.game.base.player.model.Player;
import com.slg.game.net.ToClientPacketUtil;
import com.slg.net.message.clientmessage.scene.packet.SM_EnterScene;
import com.slg.net.rpc.anno.RpcRef;
import com.slg.net.rpc.impl.scene.ISceneOptionRpcService;

import java.util.concurrent.CompletionException;

/**
 * 有真实场景实体的场景，场景的主要逻辑在scene包，需要通过rpc建立通信
 * 如SLG大地图，赛季图，副本图等
 *
 * @author yangxunan
 * @date 2026/2/2
 */
public abstract class RealSceneHandler extends AbstractSceneHandler{

    @RpcRef
    private ISceneOptionRpcService sceneRpcService;

    @Override
    public void verifyEnter(Player player, int sceneServerId, int sceneId){

        if (!verifyEnterLocal(player, sceneId)) {
            return;
        }
        if (sceneServerId <= 0){
            sceneServerId = getSceneServerId(player, sceneId);
        }

        try {
            int result = sceneRpcService.verifyEnterScene(sceneServerId, player.getId(), sceneId).join();
            // 通知客户端
            if (result == 0){
                // 可以切图，更新切图缓存数据
                player.getSceneContext().updateGoingScene(sceneServerId, sceneId);
            }
            ToClientPacketUtil.send(player, SM_EnterScene.valueOf(result));
        } catch (CompletionException e) {
            LoggerUtil.error("[场景] 验证切图 RPC 调用异常", e.getCause());
        }
    }

    @Override
    public void enterScene(Player player, int sceneServerId, int sceneId) {

        beofreEnterScene(player, sceneServerId, sceneId);

        int result = -1;
        try {
            result = sceneRpcService.enterScene(sceneServerId, player.getId(), sceneId).join();
        } catch (CompletionException e) {
            LoggerUtil.error("[场景] 切图 RPC 调用异常", e.getCause());
        }
        if (result == 0) {
            // 切图成功，更新场景上下文
            player.getSceneContext().updateScene(sceneServerId, sceneId);
        } else {
            // 切图失败
            LoggerUtil.error("[场景] 切图失败，错误码: {}", result);
            exitScene(player, sceneServerId, sceneId);
            // todo 不能直接进入原来的场景 要根据错误码来判断
            // 重新进入原来的场景
            // enterScene(player, player.getSceneContext().getCurrentSceneId(), player.getSceneContext().getCurrentSceneServerId());
        }
    }

    @Override
    public void exitScene(Player player, int sceneServerId, int sceneId){
        sceneRpcService.exitScene(player.getId(), sceneId);
    }

    /**
     * 当客户端选择默认场景时
     * 通过场景id获取场景所在的场景服
     *
     * @param player
     * @param sceneId
     * @return
     */
    public abstract int getSceneServerId(Player player, int sceneId);

    /**
     * game进入场景的校验
     * @param player
     * @param sceneId
     * @return
     */
    public abstract boolean verifyEnterLocal(Player player, int sceneId);

    /**
     * 预留接口 在进入场景之前的预处理
     * 如初次进入副本场景时，需要初始化玩家在副本的临时主城
     * @param player
     * @param sceneServerId
     * @param sceneId
     */
    public abstract void beofreEnterScene(Player player, int sceneServerId, int sceneId);
}
