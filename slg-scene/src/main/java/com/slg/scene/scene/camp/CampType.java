package com.slg.scene.scene.camp;

import lombok.Getter;

/**
 * 阵营关系类型
 * <p>定义场景中不同对象之间的阵营关系</p>
 * 
 * <p><b>关系类型：</b></p>
 * <ul>
 *   <li><b>SELF</b>：自己，不可攻击但可驻防（用于自我治疗、回城等）</li>
 *   <li><b>FRIENDLY</b>：友方（同联盟、友好NPC），不可攻击但可驻防</li>
 *   <li><b>ENEMY</b>：敌人，可攻击且不可驻防</li>
 *   <li><b>NEUTRAL</b>：中立，可攻击但不可驻防</li>
 * </ul>
 * 
 * <p><b>规则说明：</b></p>
 * <ul>
 *   <li>可攻击：ENEMY（敌人）、NEUTRAL（中立）</li>
 *   <li>不可攻击：SELF（自己）、FRIENDLY（友方）</li>
 *   <li>可驻防：SELF（自己）、FRIENDLY（友方）</li>
 *   <li>不可驻防：ENEMY（敌人）、NEUTRAL（中立）</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
@Getter
public enum CampType {

    /** 自己（不可攻击，可驻防） */
    SELF(false, true),
    
    /** 友方（同联盟、友好NPC，不可攻击，可驻防） */
    FRIENDLY(false, true),
    
    /** 敌人（明确标记为敌对，可攻击，不可驻防） */
    ENEMY(true, false),
    
    /** 中立（默认关系，可攻击，不可驻防） */
    NEUTRAL(true, false);

    /** 是否可以攻击 */
    private final boolean canAttack;
    
    /** 是否可以驻防 */
    private final boolean canGarrison;

    CampType(boolean canAttack, boolean canGarrison) {
        this.canAttack = canAttack;
        this.canGarrison = canGarrison;
    }

}
