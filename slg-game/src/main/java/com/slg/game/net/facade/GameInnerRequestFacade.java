package com.slg.game.net.facade;

import com.slg.game.net.manager.InnerSessionManager;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionRequest;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionResponce;
import com.slg.net.socket.model.NetSession;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/2/24
 */
@Component
public class GameInnerRequestFacade {

    /**
     * 处理其他服务器连接到 Game RPC 服务端的注册请求
     * 保持使用单参数 valueOf(0)，不传 needReInit
     */
    @MessageHandler
    public void socketRegisterRequest(NetSession netSession, IM_RegisterSessionRequest request) {
        InnerSessionManager.getInstance().registerSession(request.getSourceServerId(), netSession);
        netSession.sendMessage(IM_RegisterSessionResponce.valueOf(0));
    }

}
