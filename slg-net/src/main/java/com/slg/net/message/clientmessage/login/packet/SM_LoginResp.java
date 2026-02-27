package com.slg.net.message.clientmessage.login.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 登录响应消息
 * 服务端发送给客户端的登录响应
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
@Getter
@Setter
public class SM_LoginResp {
    
    /**
     * 响应码（0 表示成功，非 0 表示失败）
     */
    private int code;

    /** 登录成功时的玩家 id，失败时为 0 */
    private long playerId;

    public static SM_LoginResp valueOf(int code, long playerId){
        SM_LoginResp sm = new SM_LoginResp();
        sm.code = code;
        sm.playerId = playerId;
        return sm;
    }

}




