package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 战报成员模块 VO。
 * 用于集结战等场景，描述进攻方与防守方各成员（拥有者 + 兵种）信息。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberModuleVO extends ReportModuleVO {

    /** 进攻方成员列表 */
    private FightMemberVO[] attackerMembers;

    /** 防守方成员列表 */
    private FightMemberVO[] defenderMembers;

}
