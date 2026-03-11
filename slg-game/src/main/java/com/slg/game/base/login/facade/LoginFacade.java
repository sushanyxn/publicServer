package com.slg.game.base.login.facade;

import com.slg.game.base.login.service.LoginService;
import com.slg.game.base.player.model.Player;
import com.slg.net.message.clientmessage.login.packet.CM_LoginFinish;
import com.slg.net.message.clientmessage.login.packet.CM_LoginReq;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录模块 Facade
 * 处理 CM_LoginReq、CM_LoginFinish；完整流程为 CM_LoginReq -> SM_LoginResp -> CM_LoginFinish
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
}
