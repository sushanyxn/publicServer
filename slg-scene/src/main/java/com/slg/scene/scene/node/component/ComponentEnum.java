package com.slg.scene.scene.node.component;

import lombok.Getter;

/**
 * node 组件枚举
 * 使用组件枚举注册到主键容器
 *
 * @author yangxunan
 * @date 2026/2/5
 */
@Getter
public enum ComponentEnum {

    /** 行军组件：选择目标并朝目标移动 */
    SelectTarget("行军组件"),

    /** 阻挡组件：静态节点占格产生动态阻挡 */
    Block("阻挡组件"),

    /** 交互组件：可被行军线等交互（如攻击、采集） */
    Interactive("交互组件"),

    /** 发呆组件：行军无目标到达时使用（如到达空地、或目标无交互能力） */
    Idle("发呆组件"),

    /** 军队详情组件：描述军队信息并生成 ArmyVO（玩家军队详情/多军队详情共用此类型） */
    ArmyDetail("军队详情组件"),

    /** 集结组件：集结军队的集结等待、集结出发等业务 */
    Assemble("集结组件"),

    /** 驻防组件：静态节点上的驻守军队，使用 RouteNode 存储 */
    Garrison("驻防组件"),

    /** 战斗组件：静态节点参与战斗时生成战斗数据并提交战斗任务（来访军队 vs 驻防军队） */
    Fight("战斗组件"),

    /** 销毁组件：节点被销毁/解散时处理（如军队回城时由具体实现执行解散逻辑） */
    Destroy("销毁组件"),

    /** 战报组件：声明本节点参与战斗时需要的战报模块，用于聚合生成 ReportVO */
    Report("战报组件"),

    ;

    private final String desc;

    ComponentEnum(String desc) {
        this.desc = desc;
    }

}
