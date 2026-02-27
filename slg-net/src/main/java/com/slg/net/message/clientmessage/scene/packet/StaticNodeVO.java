package com.slg.net.message.clientmessage.scene.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 静态节点 VO（抽象基类）
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class StaticNodeVO extends SceneNodeVO {

    /** 节点所在地图格子位置 */
    private PositionVO position;

}
