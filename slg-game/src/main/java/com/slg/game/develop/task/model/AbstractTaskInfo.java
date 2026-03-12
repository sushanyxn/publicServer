package com.slg.game.develop.task.model;

import com.slg.common.log.LoggerUtil;
import com.slg.sharedmodules.progress.model.ProgressMeta;
import com.slg.sharedmodules.progress.table.IProgressTable;
import com.slg.game.SpringContext;
import com.slg.game.base.player.model.Player;
import com.slg.game.develop.task.manager.TaskManager;
import com.slg.game.net.ToClientPacketUtil;
import com.slg.net.message.clientmessage.task.packet.SM_UpdateTask;
import com.slg.net.message.clientmessage.task.packet.TaskVO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 任务信息抽象类
 * 定义了任务的通用行为和数据结构
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Getter
@Setter
public abstract class AbstractTaskInfo {

    protected TaskContainer taskContainer;

    /**
     * 初始化任务系统
     *
     * @param player 玩家
     */
    public void init(Player player) {
        // 注册所有进行中的任务到进度管理系统
        // 使用已有的 ProgressMeta，保留进度信息
        taskContainer.getTasks().values().forEach(progressMeta -> {
            IProgressTable table = progressMeta.getType().getTable(progressMeta.getId());
            SpringContext.getTaskManager().registerTask(player, taskContainer, table, progressMeta);
        });
    }

    /**
     * 接受新任务
     * 创建新的任务并注册到进度管理系统
     * 
     * @param player 玩家
     * @param table 任务表配置
     */
    public void acceptTask(Player player, IProgressTable table) {
        long taskId = table.getProgressId();
        
        // 检查任务是否已存在
        if (taskContainer.getTasks().containsKey(taskId) ||
            taskContainer.getFinishedTasks().contains(taskId) ||
            taskContainer.getGainedTasks().contains(taskId)) {
            LoggerUtil.warn("接受任务失败: 任务已存在, playerId={}, taskId={}", player.getId(), taskId);
            return;
        }

        // 创建新任务的 ProgressMeta
        ProgressMeta progressMeta = new ProgressMeta();
        progressMeta.setId(table.getProgressId());
        progressMeta.setType(table.getProgressType());

        // 添加到任务容器
        taskContainer.getTasks().put(progressMeta.getId(), progressMeta);

        // 注册到进度管理系统
        SpringContext.getTaskManager().registerTask(player, taskContainer, table, progressMeta);
    }

    /**
     * 推送任务更新
     * 在任务进度更新或完成时调用
     * 
     * @param player 玩家
     * @param progressMeta 进度元数据
     */
    public void pushTaskUpdate(Player player, ProgressMeta progressMeta) {
        TaskVO taskVO = createTaskVO(progressMeta);
        SM_UpdateTask msg = SM_UpdateTask.valueOf(taskVO);
        ToClientPacketUtil.send(player, msg);
    }

    /**
     * 推送所有任务信息
     * 在玩家登录或请求任务列表时调用，子类需要实现具体的推送逻辑
     * 
     * @param player 玩家
     */
    public abstract void pushAllTasks(Player player);

    /**
     * 创建单独任务的vo
     * @param progressMeta 进度元数据
     * @return 任务VO
     */
    public TaskVO createTaskVO(ProgressMeta progressMeta) {
        boolean finished = taskContainer.getFinishedTasks().contains(progressMeta.getId());
        boolean gained = taskContainer.getGainedTasks().contains(progressMeta.getId());

        return TaskVO.valueOf(progressMeta.getId(), progressMeta.getTypeId(), progressMeta.getProgress(), finished, gained);
    }

    /**
     * 客户端是否需要展示已经完成的任务
     * @return 是否展示已领取任务
     */
    public abstract boolean showGainedTask();

    /**
     * 构造所有任务的推送信息
     * @return 任务VO列表
     */
    public List<TaskVO> buildTasks(){
        List<TaskVO> taskVOs = new java.util.ArrayList<>();

        // 添加进行中的任务
        for (ProgressMeta progressMeta : taskContainer.getTasks().values()) {
            taskVOs.add(createTaskVO(progressMeta));
        }

        // 添加已完成的任务
        for (Integer finishedTaskId : taskContainer.getFinishedTasks()) {
            taskVOs.add(TaskVO.valueOf(finishedTaskId, taskContainer.getType().getType(), 0, true, false));
        }

        // 如果需要展示已领取的任务
        if (showGainedTask()) {
            for (Integer gainedTaskId : taskContainer.getGainedTasks()) {
                taskVOs.add(TaskVO.valueOf(gainedTaskId, taskContainer.getType().getType(), 0, true, true));
            }
        }

        return taskVOs;
    }

}
