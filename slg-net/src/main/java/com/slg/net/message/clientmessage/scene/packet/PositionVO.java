package com.slg.net.message.clientmessage.scene.packet;

import lombok.Data;

/**
 * 位置 VO
 * 
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
public class PositionVO {

    /** 格子 x 坐标 */
    private int x;

    /** 格子 y 坐标 */
    private int y;

}
