package com.slg.net.message.clientmessage.task.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 任务值对象
 * 客户端显示任务信息的数据传输对象
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class TaskVO {

    /**
     * 任务ID
     */
    private long id;

    /**
     * 任务类型
     */
    private int type;

    /**
     * 当前进度
     */
    private long progress;

    /**
     * 是否已完成
     */
    private boolean isFinished;

    /**
     * 是否已领取奖励
     */
    private boolean isGained;

}
