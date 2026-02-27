package com.slg.net.message.clientmessage.scene.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 场景查看协议
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Getter
@Setter
public class CM_Watch {

    /**
     * 移动后的x坐标
     */
    private int x;
    /**
     * 移动后的y坐标
     */
    private int y;
    /**
     * 层级
     */
    private int layer;

}
