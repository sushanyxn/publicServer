package com.slg.scene.scene.camp.strategy;

import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.node.owner.NodeOwner;
import com.slg.scene.scene.node.owner.SceneAlliance;

/**
 * 联盟 vs 任何对象的阵营关系判断策略
 * <p><b>判断规则：</b></p>
 * <ol>
 *   <li>对方是本联盟成员 → FRIENDLY</li>
 *   <li>对方是友好联盟成员（待扩展）→ FRIENDLY</li>
 *   <li>明确敌对（待扩展）→ ENEMY</li>
 *   <li>其他情况 → NEUTRAL（默认关系）</li>
 * </ol>
 * 
 * <p><b>使用场景：</b></p>
 * <ul>
 *   <li>联盟建筑的防御判断</li>
 *   <li>联盟技能的目标筛选</li>
 *   <li>联盟领地驻防判断</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public class AllianceVsAnyStrategy implements ICampRelationStrategy {

    @Override
    public CampType judgeRelation(NodeOwner owner, NodeOwner other) {
        SceneAlliance alliance = (SceneAlliance) owner;

        // 规则1：对方是玩家且是本联盟成员
        if (other instanceof ScenePlayer) {
            ScenePlayer player = (ScenePlayer) other;
            if (player.getAllianceId() == alliance.getId()) {
                return CampType.FRIENDLY;
            }
        }

        // 规则2：对方是联盟
        if (other instanceof SceneAlliance otherAlliance) {
            if (alliance.getId() == otherAlliance.getId()) {
                return CampType.SELF;
            }
            // 检查外交关系（待扩展）
            // if (AllianceRelationManager.isFriendly(alliance.getId(), otherAlliance.getId())) {
            //     return CampType.FRIENDLY;
            // }
            // if (AllianceRelationManager.isEnemy(alliance.getId(), otherAlliance.getId())) {
            //     return CampType.ENEMY;
            // }
        }

        // 规则3：默认中立
        return CampType.NEUTRAL;
    }

    @Override
    public boolean support(Class<? extends NodeOwner> ownerClass, Class<? extends NodeOwner> otherClass) {
        return ownerClass == SceneAlliance.class;
    }

}
