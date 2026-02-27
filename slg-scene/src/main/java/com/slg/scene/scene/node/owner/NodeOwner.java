package com.slg.scene.scene.node.owner;

import com.slg.net.message.clientmessage.scene.packet.OwnerVO;
import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.camp.CampUtil;
import lombok.Getter;

/**
 * 场景节点拥有者基类
 * <p>场景中的对象（城市、军队、资源点等）都有拥有者</p>
 * 
 * <p><b>子类：</b></p>
 * <ul>
 *   <li><b>ScenePlayer</b>：玩家拥有者</li>
 *   <li><b>SceneAlliance</b>：联盟拥有者</li>
 *   <li><b>NpcOwner</b>：中立NPC拥有者</li>
 *   <li><b>FriendlyNpcOwner</b>：友好NPC拥有者</li>
 * </ul>
 * 
 * <p><b>阵营关系判断：</b></p>
 * <ul>
 *   <li>通过 {@link CampUtil} 基于策略模式判断不同类型组合的阵营关系</li>
 *   <li>提供便捷方法简化业务代码</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
public abstract class NodeOwner {

    /** 拥有者ID（玩家ID、联盟ID、NPC ID等） */
    protected long id;

    /**
     * 获取与另一个 Owner 的阵营关系
     *
     * @param other 另一个拥有者
     * @return 阵营关系
     */
    public CampType getCampRelation(NodeOwner other) {
        return CampUtil.getCampRelation(this, other);
    }

    /**
     * 转换为协议用 OwnerVO，由子类实现具体类型（如 PlayerOwnerVO、AllianceOwnerVO）。
     *
     * @return 对应子类的 OwnerVO
     */
    public abstract OwnerVO toOwnerVO();

}
