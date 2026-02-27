package com.slg.net.message.clientmessage.report.packet;

import java.util.Map;

/**
 * 战斗属性 VO。
 * 描述一方在战斗中的属性：常规展示属性与额外加成属性（属性类型 id -> 数值）。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
public class FightAttributeVO {

    /** 常规展示属性，属性类型 id -> 数值 */
    private Map<Integer, Integer> showAttributes;

    /** 额外加成属性，属性类型 id -> 数值 */
    private Map<Integer, Integer> extraAttributes;

}
