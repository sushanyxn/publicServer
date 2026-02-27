package com.slg.scene.scene.camp.strategy;

import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.node.owner.NodeOwner;
import com.slg.scene.scene.node.owner.SceneAlliance;

/**
 * 玩家 vs 联盟的阵营关系判断策略
 * <p><b>判断规则：</b></p>
 * <ol>
 *   <li>玩家属于该联盟 → FRIENDLY</li>
 *   <li>外交关系友好（待扩展）→ FRIENDLY</li>
 *   <li>明确敌对（待扩展）→ ENEMY</li>
 *   <li>其他情况 → NEUTRAL（默认关系）</li>
 * </ol>
 * 
 * <p><b>使用场景：</b></p>
 * <ul>
 *   <li>玩家攻击联盟建筑（联盟领地、联盟旗帜等）</li>
 *   <li>联盟技能效果判断</li>
 *   <li>联盟领地驻防判断</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public class PlayerVsAllianceStrategy implements ICampRelationStrategy {

    @Override
    public CampType judgeRelation(NodeOwner owner, NodeOwner other) {
        ScenePlayer player = (ScenePlayer) owner;
        SceneAlliance alliance = (SceneAlliance) other;

        // 规则1：玩家属于该联盟
        if (player.getAllianceId() == alliance.getId()) {
            return CampType.FRIENDLY;
        }

        // 规则2：检查外交关系（可选，待实现）
        // if (AllianceRelationManager.isFriendly(player.getAllianceId(), alliance.getId())) {
        //     return CampType.FRIENDLY;
        // }

        // 规则3：检查是否明确敌对（待实现）
        // 例如：联盟宣战等
        // if (AllianceRelationManager.isEnemy(player.getAllianceId(), alliance.getId())) {
        //     return CampType.ENEMY;
        // }

        // 规则4：默认中立
        return CampType.NEUTRAL;
    }

    @Override
    public boolean support(Class<? extends NodeOwner> ownerClass, Class<? extends NodeOwner> otherClass) {
        return ownerClass == ScenePlayer.class && otherClass == SceneAlliance.class;
    }

}
