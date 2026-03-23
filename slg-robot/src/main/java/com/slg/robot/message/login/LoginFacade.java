package com.slg.robot.message.login;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.clientmessage.login.packet.CM_Heartbeat;
import com.slg.net.message.clientmessage.login.packet.SM_Heartbeat;
import com.slg.net.message.clientmessage.login.packet.SM_LoginResp;
import com.slg.net.message.clientmessage.scene.packet.CM_Watch;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import com.slg.robot.core.model.Robot;
import org.springframework.stereotype.Component;

/**
 * 机器人登录与心跳消息处理
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Component
public class LoginFacade {

    private static final int HEARTBEAT_INTERVAL = 30;

    @MessageHandler
    public void login(NetSession session, SM_LoginResp loginResp, Robot robot) {

        LoggerUtil.info("机器人{}登录", robot.getAccount());

        CM_Watch cmWatch = new CM_Watch();
        cmWatch.setX(500);
        cmWatch.setY(500);
        cmWatch.setLayer(1);
        robot.sendMsg(cmWatch);

        session.startHeartbeat(() -> {
            CM_Heartbeat h = new CM_Heartbeat();
            h.setClientTimestamp(System.currentTimeMillis());
            return h;
        }, HEARTBEAT_INTERVAL);
    }

    /**
     * 处理服务端心跳响应
     */
    @MessageHandler
    public void onHeartbeat(NetSession session, SM_Heartbeat resp, Robot robot) {
        LoggerUtil.debug("机器人 {} 心跳 RTT={}ms", robot.getAccount(),
                System.currentTimeMillis() - resp.getClientTimestamp());
    }

}
