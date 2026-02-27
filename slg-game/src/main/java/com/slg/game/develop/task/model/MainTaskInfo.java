package com.slg.game.develop.task.model;

import com.slg.game.base.player.model.Player;
import com.slg.game.core.progress.GameProgressType;
import com.slg.game.net.ToClientPacketUtil;
import com.slg.net.message.clientmessage.task.packet.SM_MainTaskInfo;
import com.slg.net.message.clientmessage.task.packet.TaskVO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 主线任务信息
 * 保存玩家的主线任务进度数据
 *
 * @author yangxunan
 * @date 2026/1/29
 */
@Getter
@Setter
public class MainTaskInfo extends AbstractTaskInfo {

    /**
     * 构造函数，初始化任务容器
     */
    public MainTaskInfo() {
        this.taskContainer = new TaskContainer();
        this.taskContainer.setType(GameProgressType.Main);
    }

    /**
     * 推送主线任务列表
     * @param player 玩家
     */
    @Override
    public void pushAllTasks(Player player) {
        List<TaskVO> tasks = buildTasks();
        SM_MainTaskInfo msg = SM_MainTaskInfo.valueOf(tasks);
        ToClientPacketUtil.send(player, msg);
    }

    /**
     * 主线任务不展示已领取的任务
     * @return false
     */
    @Override
    public boolean showGainedTask(){
        return false;
    }
}
