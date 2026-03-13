package com.slg.net.message.clientmessage.gm.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GM 指令执行结果
 * 服务端返回给客户端的 GM 执行结果
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class SM_GMResult {

    /** 原始指令 */
    private String command;

    /** 错误码：0 成功，1 失败 */
    private int code;

    /** 结果描述 */
    private String message;

}
