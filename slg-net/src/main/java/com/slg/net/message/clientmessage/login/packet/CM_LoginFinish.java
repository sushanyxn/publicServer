package com.slg.net.message.clientmessage.login.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 登录完成消息
 * 客户端在收到 SM_LoginResp 成功后发送，表示正式完成登录；服务端在此后抛出登录事件
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Getter
@Setter
public class CM_LoginFinish {
}
