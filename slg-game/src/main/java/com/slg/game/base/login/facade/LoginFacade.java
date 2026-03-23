package com.slg.game.base.login.facade;

import com.slg.game.base.login.service.LoginService;
import com.slg.game.base.player.model.Player;
import com.slg.net.message.clientmessage.login.packet.CM_Heartbeat;
import com.slg.net.message.clientmessage.login.packet.CM_LoginFinish;
import com.slg.net.message.clientmessage.login.packet.CM_LoginReq;
import com.slg.net.message.clientmessage.login.packet.SM_Heartbeat;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录模块 Facade
 * 处理登录流程（CM_LoginReq -> SM_LoginResp -> CM_LoginFinish）和客户端心跳
 *
 * @author yangxunan
 * @date 2026/1/21
 */
@Component
public class LoginFacade {

    @Autowired
    private LoginService loginService;

    @MessageHandler
    public void login(NetSession session, CM_LoginReq loginReq) {
        loginService.login(session, loginReq);
    }

    @MessageHandler
    public void loginFinish(NetSession session, CM_LoginFinish packet, Player player) {
        loginService.loginFinish(session, packet, player);
    }

    /**
     * 处理客户端心跳，回传时间戳用于 RTT 计算
     */
    @MessageHandler
    public void heartbeat(NetSession session, CM_Heartbeat req, Player player) {
        SM_Heartbeat resp = new SM_Heartbeat();
        resp.setServerTimestamp(System.currentTimeMillis());
        resp.setClientTimestamp(req.getClientTimestamp());
        session.sendMessage(resp);
    }
}
