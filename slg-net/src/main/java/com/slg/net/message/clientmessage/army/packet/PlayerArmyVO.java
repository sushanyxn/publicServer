package com.slg.net.message.clientmessage.army.packet;

import com.slg.net.message.clientmessage.hero.packet.HeroVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 玩家军队 VO
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerArmyVO extends ArmyVO {

    /** 军队内英雄列表 */
    private HeroVO[] heroes;

    /** 军队内兵种列表（当前逻辑：所有英雄共同带领一队兵） */
    private TroopVO[] troops;

}
