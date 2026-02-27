package com.slg.net.message.clientmessage.army.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 世界 Boss 军队 VO
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorldBossArmyVO extends ArmyVO {

    /** 世界 Boss 配置 id */
    private int configId;

}
