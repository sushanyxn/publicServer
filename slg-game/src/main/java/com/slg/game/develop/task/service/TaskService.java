package com.slg.game.develop.task.service;

import com.slg.common.bean.IReward;
import com.slg.common.log.LoggerUtil;
import com.slg.common.progress.table.IProgressTable;
import com.slg.game.base.player.model.Player;
import com.slg.game.core.progress.GameProgressType;
import com.slg.game.develop.task.manager.TaskManager;
import com.slg.game.develop.task.model.TaskContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 任务业务服务
 * 处理任务相关的业务逻辑
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Component
public class TaskService {

    @Autowired
    private TaskManager taskManager;

    /**
     * 根据任务id领取奖励
     * @param player 玩家
     * @param progressType 任务类型
     * @param id 任务ID
     */
    public void gainTask(Player player, GameProgressType progressType, int id){
        TaskContainer taskContainer = taskManager.getTaskContainer(player, progressType);

        // 检查任务是否已完成
        if (!taskContainer.getFinishedTasks().contains(id)) {
            LoggerUtil.warn("领取任务奖励失败: 任务未完成, playerId={}, type={}, taskId={}", 
                player.getId(), progressType.getType(), id);
            return;
        }

        // 检查任务是否已领取
        if (taskContainer.getGainedTasks().contains(id)) {
            LoggerUtil.warn("领取任务奖励失败: 任务已领取, playerId={}, type={}, taskId={}", 
                player.getId(), progressType.getType(), id);
            return;
        }

        // 获取任务表配置
        IProgressTable table = progressType.getTable(id);

        // 发放奖励
        IReward<Player> reward = table.getReward();
        if (reward != null) {
            reward.reward(player);
        }

        // 标记为已领取
        taskContainer.getFinishedTasks().remove(id);
        taskContainer.getGainedTasks().add(id);

        LoggerUtil.debug("领取任务奖励成功: playerId={}, type={}, taskId={}", 
            player.getId(), progressType.getType(), id);
    }


}
