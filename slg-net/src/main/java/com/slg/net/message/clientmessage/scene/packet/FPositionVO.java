package com.slg.net.message.clientmessage.scene.packet;

import lombok.Data;

/**
 * 亚格子位置 VO（定点数，放大 100 倍）
 * <p>使用 int 存储，实际坐标 = 值 / 100，用于中心点等需要亚格子精度的协议字段。</p>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
@Data
public class FPositionVO {

    /** X 坐标（放大 100 倍） */
    private int x;

    /** Y 坐标（放大 100 倍） */
    private int y;
}
