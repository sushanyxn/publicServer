package com.slg.game.develop.task.table;

import com.slg.common.bean.IReward;
import com.slg.common.progress.bean.IProgressCondition;
import com.slg.common.progress.model.IProgressEvent;
import com.slg.common.progress.table.IProgressTable;
import com.slg.common.progress.type.ProgressTypeEnum;
import com.slg.game.core.progress.GameProgressType;
import com.slg.table.anno.Table;
import com.slg.table.anno.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 主线任务表
 * 配置主线任务的条件、奖励等信息
 * 
 * @author yangxunan
 * @date 2026/1/29
 */
@Table
@Getter
@Setter
public class MainTaskTable implements IProgressTable {

    /**
     * 任务ID
     */
    @TableId
    private int id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 进度条件（JSON配置）
     */
    private IProgressCondition<?, ?> progressCondition;

    /**
     * 任务奖励（JSON配置）
     */
    private IReward<?> reward;

    @Override
    public int getProgressId() {
        return id;
    }

    @Override
    public ProgressTypeEnum getProgressType() {
        return GameProgressType.Main;
    }

    @Override
    public boolean isAutoGain() {
        return false;
    }
}
