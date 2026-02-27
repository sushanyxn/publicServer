package com.slg.net.message.clientmessage.scene.packet;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 请求进入场景的返回
 *
 * @author yangxunan
 * @date 2026/2/2
 */
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class SM_EnterScene {

    /**
     * 0 = 校验通过，可以切图
     * 其他 = 错误码
     */
    private int result;

}
