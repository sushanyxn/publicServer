package com.slg.scene.scene.camp.strategy;

import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.node.owner.FriendlyNpcOwner;
import com.slg.scene.scene.node.owner.NodeOwner;
import com.slg.scene.scene.node.owner.NpcOwner;

/**
 * 玩家 vs NPC的阵营关系判断策略
 * <p><b>判断规则：</b></p>
 * <ol>
 *   <li>友好NPC（商人、向导等）→ FRIENDLY</li>
 *   <li>中立NPC（野怪、资源点等）→ NEUTRAL（玩家可以攻击）</li>
 *   <li>敌对NPC（侵略者等，待扩展）→ ENEMY</li>
 * </ol>
 * 
 * <p><b>注意：</b></p>
 * <ul>
 *   <li>NEUTRAL 表示玩家可以选择攻击，但NPC不会主动攻击玩家</li>
 *   <li>如果需要NPC主动攻击，应该在NPC的AI逻辑中判断</li>
 *   <li>友好NPC不可攻击且可以驻防（如城市守卫）</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public class PlayerVsNpcStrategy implements ICampRelationStrategy {

    @Override
    public CampType judgeRelation(NodeOwner owner, NodeOwner other) {
        // 规则1：友好NPC
        if (other instanceof FriendlyNpcOwner) {
            return CampType.FRIENDLY;
        }

        // 规则2：中立NPC（可以被攻击，但不会主动攻击）
        if (other instanceof NpcOwner) {
            return CampType.NEUTRAL;
        }

        // 规则3：其他类型的NPC（待扩展）
        // 例如：敌对NPC、叛军等
        // if (other instanceof HostileNpcOwner) {
        //     return CampType.ENEMY;
        // }
        
        // 规则4：默认中立
        return CampType.NEUTRAL;
    }

    @Override
    public boolean support(Class<? extends NodeOwner> ownerClass, Class<? extends NodeOwner> otherClass) {
        // 支持玩家对所有NPC类型的判断
        return ownerClass == ScenePlayer.class && 
               (otherClass == NpcOwner.class || 
                otherClass == FriendlyNpcOwner.class ||
                NpcOwner.class.isAssignableFrom(otherClass));
    }

}
