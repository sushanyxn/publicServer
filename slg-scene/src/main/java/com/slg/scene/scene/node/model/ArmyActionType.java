package com.slg.scene.scene.node.model;

import com.slg.scene.scene.node.component.impl.common.InteractiveComponent;
import com.slg.scene.scene.node.component.impl.common.SelectTargetComponent;
import lombok.Getter;

/**
 * 行军目的枚举
 * <p>选择目标（{@link SelectTargetComponent#selectTarget}）时传入并保存，
 * 到达目标后传入交互方法（{@link InteractiveComponent#onInteractedBy}），
 * 供目标方按目的执行不同逻辑（如驻扎、参与集结、敌对等）。</p>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
@Getter
public enum ArmyActionType {

    /** 缺省，未明确指定目的 */
    DEFAULT("缺省"),

    /** 驻扎：到达后驻防该节点 */
    GARRISON("驻扎"),

    /** 参与集结：到达后加入集结 */
    JOIN_ASSEMBLE("参与集结"),

    /** 敌对：到达后与目标敌对交互（如攻击、掠夺） */
    ATTACK("敌对"),
    ;

    private final String desc;

    ArmyActionType(String desc) {
        this.desc = desc;
    }
}
