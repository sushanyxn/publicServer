package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 战报属性模块 VO。
 * 描述进攻方与防守方在战斗中的属性数据（展示属性、加成属性等）。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttributeModuleVO extends ReportModuleVO {

    /** 进攻方战斗属性 */
    private FightAttributeVO attackerAttribute;

    /** 防守方战斗属性 */
    private FightAttributeVO defenderAttribute;

}
