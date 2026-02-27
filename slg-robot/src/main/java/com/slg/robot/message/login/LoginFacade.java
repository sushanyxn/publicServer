package com.slg.robot.message.login;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.clientmessage.login.packet.SM_LoginResp;
import com.slg.net.message.clientmessage.scene.packet.CM_Watch;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import com.slg.robot.core.model.Robot;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/1/22
 */
@Component
public class LoginFacade {

    @MessageHandler
    public void login(NetSession session, SM_LoginResp loginResp, Robot robot) {

        LoggerUtil.info("机器人{}登录", robot.getAccount());

        CM_Watch cmWatch = new CM_Watch();
        cmWatch.setX(500);
        cmWatch.setY(500);
        cmWatch.setLayer(1);
        robot.sendMsg(cmWatch);
    }

}
