package com.slg.net.message.clientmessage.task.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 任务更新推送
 * 服务端推送给客户端的任务进度更新消息
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class SM_UpdateTask {

    /**
     * 更新的任务信息
     */
    private TaskVO task;

}
