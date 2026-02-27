package com.slg.net.message.clientmessage.scene.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 怪物所有者 VO
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MonsterOwnerVO extends OwnerVO{

    private int configId;

}
