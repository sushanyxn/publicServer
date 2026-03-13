package com.slg.net.message.clientmessage.login.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 登录请求消息
 * 客户端发送给服务端的登录请求
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
@Getter
@Setter
public class CM_LoginReq {

    /** 账号（登录标识） */
    private String account;

    /** 玩家 id（选服/重连时可选填，0 表示新登录） */
    private long playerId;

    /** 登录令牌 */
    private String loginToken;

}




