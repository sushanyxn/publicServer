package com.slg.scene.base.service;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.model.NetSession;
import com.slg.scene.base.manager.ScenePlayerManager;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.net.manager.InnerSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Game 服断线处理器
 * 负责宽限期到期后的节点清理
 * 在 SCENE 单链线程中调用（由宽限期定时器 → Executor.Scene.execute 触发）
 *
 * @author yangxunan
 * @date 2026/02/11
 */
@Component
public class GameServerDisconnectHandler {

    @Autowired
    private ScenePlayerManager scenePlayerManager;

    /**
     * Game 服确认失联后执行清理
     * 在 SCENE 单链线程中调用（由宽限期定时器 → Executor.Scene.execute 触发）
     *
     * @param gameServerId 断开的游戏服务器ID
     */
    public void handleGameServerLost(int gameServerId) {
        // 防御性检查：如果在 Scene 链排队期间 Game 已重连，跳过清理
        // （宽限期定时器在 System 链分发到 Scene 链后、Scene 链实际执行前，可能已有新连接建立）
        NetSession currentSession = InnerSessionManager.getInstance().getSession(gameServerId);
        if (currentSession != null && currentSession.isActive()) {
            LoggerUtil.debug("[Scene断线处理] Game服{}在清理执行前已重连，跳过清理", gameServerId);
            return;
        }

        // 1. 遍历 scenePlayers，按 gameServerId 过滤出已初始化的玩家
        List<ScenePlayer> affectedPlayers = scenePlayerManager.getInitedPlayersByGameServer(gameServerId);

        LoggerUtil.debug("[Scene断线处理] Game服{}确认失联，清理{}个玩家的场景节点",
                gameServerId, affectedPlayers.size());

        // 2. 在 Scene 链中逐个清除场景节点并重置初始化状态
        for (ScenePlayer scenePlayer : affectedPlayers) {
            try {
                // todo 经历玩家在场景中的单位

                // 重置初始化标识，等待 Game 重连后重新初始化
                scenePlayer.setSceneInited(false);
            } catch (Exception e) {
                LoggerUtil.error("[Scene断线处理] 清理玩家{}的场景节点异常", scenePlayer.getId(), e);
            }
        }
    }
}
