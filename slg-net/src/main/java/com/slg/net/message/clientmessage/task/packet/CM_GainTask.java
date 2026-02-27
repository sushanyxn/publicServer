package com.slg.net.message.clientmessage.task.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 领取任务奖励请求
 * 客户端完成任务后请求领取奖励
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Getter
@Setter
public class CM_GainTask {

    /**
     * 任务类型
     */
    private int type;

    /**
     * 任务ID
     */
    private int id;

}
