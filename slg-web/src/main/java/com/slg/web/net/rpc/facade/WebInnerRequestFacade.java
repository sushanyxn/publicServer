package com.slg.web.net.rpc.facade;

import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionRequest;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionResponce;
import com.slg.net.socket.model.NetSession;
import com.slg.web.net.rpc.manager.WebInnerSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Web 服内部消息处理
 * 处理 game 服连接到 web 服后的注册请求
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class WebInnerRequestFacade {

    @Autowired
    private WebInnerSessionManager webInnerSessionManager;

    /**
     * 处理 game 服连接注册请求
     * game 服连接 web 服 RPC 端口后发送此消息，web 端记录 serverId → session 映射
     */
    @MessageHandler
    public void socketRegisterRequest(NetSession netSession, IM_RegisterSessionRequest request) {
        webInnerSessionManager.registerSession(request.getSourceServerId(), netSession);
        netSession.sendMessage(IM_RegisterSessionResponce.valueOf(0));
    }
}
