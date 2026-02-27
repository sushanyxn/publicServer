package com.slg.game.develop.task.model;

import com.slg.common.progress.model.ProgressMeta;
import com.slg.game.core.progress.GameProgressType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 任务容器
 * 保存任务的进度、完成和领取状态
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Getter
@Setter
public class TaskContainer {

    /**
     * 任务类型
     */
    private GameProgressType type;

    /**
     * 正在进行中的任务
     */
    private Map<Integer, ProgressMeta> tasks = new HashMap<Integer, ProgressMeta>();

    /**
     * 已完成，但未领取奖励的任务
     */
    private Set<Integer> finishedTasks = new HashSet<>();

    /**
     * 已经领取奖励的任务
     */
    private Set<Integer> gainedTasks = new HashSet<>();


}
