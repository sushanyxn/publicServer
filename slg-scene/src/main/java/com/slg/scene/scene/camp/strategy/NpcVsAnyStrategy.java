package com.slg.scene.scene.camp.strategy;

import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.node.owner.FriendlyNpcOwner;
import com.slg.scene.scene.node.owner.NodeOwner;
import com.slg.scene.scene.node.owner.NpcOwner;

/**
 * NPC vs 任何对象的阵营关系判断策略
 * <p><b>判断规则：</b></p>
 * <ol>
 *   <li>友好NPC对所有对象友好 → FRIENDLY</li>
 *   <li>中立NPC对所有对象中立 → NEUTRAL</li>
 *   <li>敌对NPC（待扩展）根据AI逻辑判断</li>
 * </ol>
 * 
 * <p><b>设计说明：</b></p>
 * <ul>
 *   <li>NPC通常不会主动发起攻击判断</li>
 *   <li>如果NPC需要攻击玩家，应该在AI系统中特殊处理</li>
 *   <li>此策略主要用于NPC被动响应时的阵营判断</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public class NpcVsAnyStrategy implements ICampRelationStrategy {

    @Override
    public CampType judgeRelation(NodeOwner owner, NodeOwner other) {
        // 规则1：友好NPC对所有人友好
        if (owner instanceof FriendlyNpcOwner) {
            return CampType.FRIENDLY;
        }

        // 规则2：中立NPC保持中立
        if (owner instanceof NpcOwner) {
            return CampType.NEUTRAL;
        }

        // 规则3：其他类型NPC（待扩展）
        // 例如：主动攻击的敌对NPC
        return CampType.NEUTRAL;
    }

    @Override
    public boolean support(Class<? extends NodeOwner> ownerClass, Class<? extends NodeOwner> otherClass) {
        // 支持所有NPC类型作为主体的判断
        return NpcOwner.class.isAssignableFrom(ownerClass) || 
               FriendlyNpcOwner.class.isAssignableFrom(ownerClass);
    }

}
