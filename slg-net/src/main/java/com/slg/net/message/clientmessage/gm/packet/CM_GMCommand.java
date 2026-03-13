package com.slg.net.message.clientmessage.gm.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * GM 指令请求
 * 客户端发送的 GM 指令字符串，格式为 "方法名 参数1 参数2 ..."
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
@Setter
public class CM_GMCommand {

    /** GM 指令字符串，如 "gainHero 1001" */
    private String command;

}
