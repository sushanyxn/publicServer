package com.slg.net.message.clientmessage.army.packet;

import lombok.Data;

/**
 * 军队 VO（抽象基类）
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Data
public abstract class ArmyVO {

    /** 军队实体 id（场景或全局唯一，视子类用途而定） */
    private long id;

}
