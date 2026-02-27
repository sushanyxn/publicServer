package com.slg.net.message.clientmessage.task.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 主线任务信息推送
 * 服务端推送给客户端的主线任务列表
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class SM_MainTaskInfo {

    /**
     * 主线任务列表
     */
    private List<TaskVO> tasks;

}
