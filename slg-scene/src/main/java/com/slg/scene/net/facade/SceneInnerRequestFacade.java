package com.slg.scene.net.facade;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionRequest;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionResponce;
import com.slg.net.socket.model.NetSession;
import com.slg.scene.net.manager.InnerSessionManager;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/2/24
 */
@Component
public class SceneInnerRequestFacade {

    /**
     * 处理 Game 服发来的注册请求
     * 在 System 链中执行（由 RpcServerMessageHandler.onMessage 的 else 分支分发）
     * 与 removeSession / cancelPendingCleanup 串行
     */
    @MessageHandler
    public void socketRegisterRequest(NetSession netSession, IM_RegisterSessionRequest request) {
        int gameServerId = request.getSourceServerId();
        InnerSessionManager.getInstance().registerSession(gameServerId, netSession);

        // 检查是否在宽限期内重连（取消待清理定时器）
        boolean inGracePeriod = InnerSessionManager.getInstance().cancelPendingCleanup(gameServerId);

        // needReInit: 宽限期内重连=false（节点还在），否则=true（首次连接或已清理）
        boolean needReInit = !inGracePeriod;
        LoggerUtil.debug("[Scene连接注册] Game服{}注册成功, inGracePeriod={}, needReInit={}",
                gameServerId, inGracePeriod, needReInit);

        netSession.sendMessage(IM_RegisterSessionResponce.valueOf(0, needReInit));
    }

}
