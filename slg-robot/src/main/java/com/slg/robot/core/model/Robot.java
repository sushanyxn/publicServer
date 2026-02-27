package com.slg.robot.core.model;

import com.slg.net.socket.model.NetSession;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yangxunan
 * @date 2026/1/22
 */
@Getter
@Setter
public class Robot {

    private String account;

    private long playerId;

    private NetSession session;

    public Robot(String account, long playerId) {
        this.account = account;
        this.playerId = playerId;
    }

    public void sendMsg(Object msg) {
        if (session != null && session.isActive()) {
            session.sendMessage(msg);
        }
    }

    public void bindSession(NetSession session) {
        this.session = session;
        session.setPlayerId(playerId);
    }

}
