package com.slg.net.message.clientmessage.scene.packet;

import com.slg.net.message.clientmessage.army.packet.ArmyVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 路径节点 VO
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(staticName = "valueOf")
@NoArgsConstructor
public class RouteNodeVO extends SceneNodeVO {

    /** 路径起点（亚格子坐标） */
    private FPositionVO startPosition;

    /** 路径终点（亚格子坐标） */
    private FPositionVO endPosition;

    /** 该路径节点上的军队信息 */
    private ArmyVO armyVO;

}
