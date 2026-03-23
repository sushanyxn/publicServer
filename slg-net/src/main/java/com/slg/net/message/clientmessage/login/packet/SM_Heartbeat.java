package com.slg.net.message.clientmessage.login.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 服务端心跳响应
 * 回传客户端时间戳用于 RTT 计算，同时携带服务器时间戳
 *
 * @author yangxunan
 * @date 2026/03/23
 */
@Getter
@Setter
public class SM_Heartbeat {

    /** 服务器时间戳（毫秒） */
    private long serverTimestamp;

    /** 回传客户端时间戳（用于 RTT 计算） */
    private long clientTimestamp;

}
