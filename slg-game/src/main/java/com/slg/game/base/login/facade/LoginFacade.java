package com.slg.game.base.login.facade;

import com.slg.game.base.login.service.LoginService;
import com.slg.net.message.clientmessage.login.packet.CM_LoginReq;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录模块 Facade
 * 处理客户端登录请求（CM_LoginReq）
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


}
