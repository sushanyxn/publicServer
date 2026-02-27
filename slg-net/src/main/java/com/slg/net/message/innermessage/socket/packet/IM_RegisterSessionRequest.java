package com.slg.net.message.innermessage.socket.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 内部链接注册消息
 *
 * @author yangxunan
 * @date 2026/1/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class IM_RegisterSessionRequest {

    /** 发起注册的源服 serverId */
    private int sourceServerId;

}
