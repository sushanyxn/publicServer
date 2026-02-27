package com.slg.game.develop.task.facade;

import com.slg.game.base.player.model.Player;
import com.slg.game.core.progress.GameProgressType;
import com.slg.game.develop.task.service.TaskService;
import com.slg.net.message.clientmessage.task.packet.CM_GainTask;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务模块协议处理器
 * 处理客户端发送的任务相关协议
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Component
public class TaskFacade {

    @Autowired
    private TaskService taskService;

    /**
     * 处理领取任务奖励请求
     * @param session 网络会话
     * @param req 领取任务请求
     * @param player 玩家对象（框架自动注入）
     */
    @MessageHandler
    public void gainTask(NetSession session, CM_GainTask req, Player player) {
        // 根据类型获取对应的任务类型枚举
        GameProgressType progressType = GameProgressType.getProgressType(req.getType());

        taskService.gainTask(player, progressType, req.getId());
    }

}
