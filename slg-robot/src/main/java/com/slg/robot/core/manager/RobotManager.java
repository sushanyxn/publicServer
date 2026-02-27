package com.slg.robot.core.manager;

import com.slg.net.message.clientmessage.login.packet.CM_LoginReq;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.client.WebSocketClientManager;
import com.slg.robot.core.config.RobotConfig;
import com.slg.robot.core.model.Robot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangxunan
 * @date 2026/1/22
 */
@Component
public class RobotManager {

    @Autowired
    private RobotConfig robotConfig;

    private Map<Long, Robot> robots = new ConcurrentHashMap<>();

    public Robot getRobot(long robotId) {
        return robots.get(robotId);
    }

    public void createRobot(int index, long robotId){

        String name = "Robot_" + index;
        Robot robot = new Robot(name, robotId);
        robots.put(robotId, robot);

        NetSession session = WebSocketClientManager.getInstance().connect(robotConfig.getServerUrl());
        robot.bindSession(session);
        CM_LoginReq loginReq = new CM_LoginReq();
        loginReq.setAccount(robot.getAccount());
        loginReq.setPlayerId(robotId);
        robot.sendMsg(loginReq);

    }

}
