package com.slg.net.message.clientmessage.login.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户端心跳请求
 * 客户端定时发送，服务端收到后回复 SM_Heartbeat
 *
 * @author yangxunan
 * @date 2026/03/23
 */
@Getter
@Setter
public class CM_Heartbeat {

    /** 客户端发送时间戳（毫秒） */
    private long clientTimestamp;

}
