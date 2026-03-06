package com.slg.game.net.facade;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.game.base.player.manager.PlayerManager;
import com.slg.game.base.player.model.SceneServerContext;
import com.slg.game.base.player.service.PlayerService;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionResponce;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Game 服内部响应 Facade
 * 处理 Scene 服返回的会话注册响应（IM_RegisterSessionResponce）
 *
 * @author yangxunan
 * @date 2026/2/24
 */
@Component
public class GameInnerResponseFacade {

    @Autowired
    private PlayerManager playerManager;
    @Autowired
    private PlayerService playerService;

    /**
     * 处理 Scene 服返回的注册响应
     * 先将 ConnectState 设为 READY（纯连接状态），再根据 needReInit 决定是否触发批量初始化
     */
    @MessageHandler
    public void socketRegisterResponse(NetSession netSession, IM_RegisterSessionResponce response) {
        if (response.getResult() == 0) {
            int sceneServerId = netSession.getServerId();
            LoggerUtil.debug("与服务器{}的连接建立完成, needReInit={}", sceneServerId, response.isNeedReInit());

            // 1. 注册成功 → 连接就绪（ConnectState 是纯服务器状态，无论是否需要重新初始化都设为 READY）
            SceneServerContext ctx = playerManager.getSceneServerContextMap().get(sceneServerId);
            if (ctx != null) {
                ctx.getConnectState().set(SceneServerContext.ConnectState.READY);
            }

            // 2. 根据 needReInit 决定是否批量初始化
            if (response.isNeedReInit()) {
                // 首次连接或宽限期已过（Scene 已清理），需要批量初始化
                Executor.System.execute(() -> {
                    playerService.batchInitPlayerScene(sceneServerId);
                });
            }
            // needReInit=false：Scene 节点仍在，玩家各自的 sceneInit 状态保持为 true，无需额外操作
        }
    }

}
