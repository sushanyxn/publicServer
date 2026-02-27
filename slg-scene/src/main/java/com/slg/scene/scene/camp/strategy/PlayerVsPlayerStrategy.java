package com.slg.scene.scene.camp.strategy;

import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.node.owner.NodeOwner;

/**
 * 玩家 vs 玩家的阵营关系判断策略
 * <p><b>判断规则：</b></p>
 * <ol>
 *   <li>同一个玩家 → SELF</li>
 *   <li>同联盟 → FRIENDLY</li>
 *   <li>外交关系友好（待扩展）→ FRIENDLY</li>
 *   <li>明确敌对（待扩展）→ ENEMY</li>
 *   <li>其他情况 → NEUTRAL（默认关系）</li>
 * </ol>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public class PlayerVsPlayerStrategy implements ICampRelationStrategy {

    @Override
    public CampType judgeRelation(NodeOwner owner, NodeOwner other) {
        ScenePlayer player1 = (ScenePlayer) owner;
        ScenePlayer player2 = (ScenePlayer) other;

        // 规则1：同一个玩家
        if (player1.getId() == player2.getId()) {
            return CampType.SELF;
        }

        // 规则2：同联盟
        if (player1.getAllianceId() > 0 && player1.getAllianceId() == player2.getAllianceId()) {
            return CampType.FRIENDLY;
        }

        // 规则3：检查外交关系（可选，待实现）
        // 例如：联盟之间的外交协议（盟友、停战等）
        // if (AllianceRelationManager.isFriendly(player1.getAllianceId(), player2.getAllianceId())) {
        //     return CampType.FRIENDLY;
        // }

        // 规则4：检查是否明确敌对（待实现）
        // 例如：联盟宣战、仇恨系统等
        // if (AllianceRelationManager.isEnemy(player1.getAllianceId(), player2.getAllianceId())) {
        //     return CampType.ENEMY;
        // }

        // 规则5：默认中立（除非明确标记为友方或敌人，否则都是中立）
        return CampType.NEUTRAL;
    }

    @Override
    public boolean support(Class<? extends NodeOwner> ownerClass, Class<? extends NodeOwner> otherClass) {
        return ownerClass == ScenePlayer.class && otherClass == ScenePlayer.class;
    }

}
