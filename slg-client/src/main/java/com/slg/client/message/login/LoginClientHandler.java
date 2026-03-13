package com.slg.client.message.login;

import com.slg.client.core.account.ClientAccount;
import com.slg.client.ui.MainWindow;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.clientmessage.login.packet.CM_LoginFinish;
import com.slg.net.message.clientmessage.login.packet.SM_LoginResp;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 客户端登录消息处理
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class LoginClientHandler {

    @Autowired
    private MainWindow mainWindow;

    /**
     * 处理服务端登录响应
     */
    @MessageHandler
    public void login(NetSession session, SM_LoginResp loginResp, ClientAccount account) {
        if (loginResp.getCode() != 0) {
            LoggerUtil.warn("账号 {} 登录失败，错误码: {}", account.getAccount(), loginResp.getCode());
            return;
        }

        account.onLoginSuccess(loginResp.getPlayerId());
        session.setPlayerId(loginResp.getPlayerId());

        LoggerUtil.info("账号 {} 登录成功，playerId={}", account.getAccount(), loginResp.getPlayerId());

        account.sendMessage(new CM_LoginFinish());

        mainWindow.onLoginSuccess(account);
    }
}
