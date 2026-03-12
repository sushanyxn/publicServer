package com.slg.game.develop.task.manager;

import com.slg.sharedmodules.progress.manager.ProgressManager;
import com.slg.sharedmodules.progress.model.ProgressMeta;
import com.slg.sharedmodules.progress.table.IProgressTable;
import com.slg.sharedmodules.progress.type.ProgressOwnerEnum;
import com.slg.game.base.player.model.Player;
import com.slg.game.core.progress.GameProgressType;
import com.slg.game.develop.task.model.AbstractTaskInfo;
import com.slg.game.develop.task.model.MainTaskInfo;
import com.slg.game.develop.task.model.TaskContainer;
import com.slg.game.develop.task.service.TaskService;
import com.slg.game.develop.task.table.MainTaskTable;
import com.slg.table.anno.Table;
import com.slg.table.model.TableInt;
import com.slg.table.model.TableLong;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 通用任务数据管理类
 *
 * @author yangxunan
 * @date 2026/1/29
 */
@Component
@Getter
public class TaskManager {

    @Autowired
    private ProgressManager progressManager;

    @Lazy
    @Autowired
    private TaskService taskService;

    @Table
    private TableInt<MainTaskTable> mainTaskTable;

    public MainTaskTable getMainTaskTable(int taskId) {
        return mainTaskTable.get(taskId);
    }

    /**
     * 注册任务到进度管理系统
     * 用于重新注册已有任务或注册新任务
     * 注意：不会自动添加到任务容器，由调用方控制
     * 
     * @param player 玩家
     * @param taskContainer 任务容器
     * @param table 任务表配置
     * @param progressMeta 进度元数据（已有的或新创建的）
     */
    public void registerTask(Player player, TaskContainer taskContainer, IProgressTable table, ProgressMeta progressMeta){
        AbstractTaskInfo taskInfo = getTaskInfo(player, taskContainer.getType());
        
        // 设置进度更新回调
        progressMeta.setWhenUpdate(meta -> taskInfo.pushTaskUpdate(player, meta));
        
        // 设置进度完成回调
        progressMeta.setWhenFinish(meta -> {
            // 任务完成，移除监听
            progressManager.unregisterProgress(ProgressOwnerEnum.Player, player.getId(), meta);
            
            // 加入完成列表，从进行中列表移除
            taskContainer.getFinishedTasks().add(meta.getId());
            taskContainer.getTasks().remove(meta.getId());

            // 推送任务完成通知
            taskInfo.pushTaskUpdate(player, meta);

            // 自动领奖判断
            if (table.isAutoGain()){
                taskService.gainTask(player, taskContainer.getType(), meta.getId());
            }
        });

        // 注册到进度管理系统
        progressManager.registerProgress(
                ProgressOwnerEnum.Player,
                player.getId(),
                player,
                table,
                progressMeta
        );
    }

    /**
     * 根据任务类型获取任务容器
     * @param player 玩家
     * @param progressType 任务类型
     * @return 任务容器
     */
    public TaskContainer getTaskContainer(Player player, GameProgressType progressType){
        return switch (progressType){
            case Main -> player.getPlayerEntity().getMainTaskInfo().getTaskContainer();
        };
    }

    /**
     * 根据任务类型获取任务信息
     * @param player 玩家
     * @param progressType 任务类型
     * @return 任务信息
     */
    public AbstractTaskInfo getTaskInfo(Player player, GameProgressType progressType){
        return switch (progressType){
            case Main -> player.getPlayerEntity().getMainTaskInfo();
        };
    }

}
