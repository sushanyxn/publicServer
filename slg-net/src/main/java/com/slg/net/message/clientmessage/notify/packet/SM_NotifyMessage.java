package com.slg.net.message.clientmessage.notify.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 服务端通知消息推送
 * 服务端向客户端推送提示信息，客户端根据 infoId 查找 MessageTable 获取显示内容
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class SM_NotifyMessage {

    /** 普通消息（弹 tips） */
    public static final int TYPE_NORMAL = 1;

    /** 消息 ID，对应 MessageTable 中的 id */
    private int infoId;

    /** 消息类型：1-普通消息，后续可扩展跑马灯等 */
    private int msgType;

}
