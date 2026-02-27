package com.slg.net.message.clientmessage.army.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 集结军队 VO
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssembleArmyVO extends ArmyVO {

    /** 集结队长玩家 id */
    private long leaderId;

    /** 集结成员军队列表 */
    private List<PlayerArmyVO> members;

}
