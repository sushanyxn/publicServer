package com.slg.sharedmodules.progress.table;

import com.slg.common.bean.IReward;
import com.slg.sharedmodules.progress.bean.IProgressCondition;
import com.slg.sharedmodules.progress.model.IProgressEvent;
import com.slg.sharedmodules.progress.type.ProgressTypeEnum;

/**
 * 进度表配置接口
 *
 * @author yangxunan
 * @date 2026/1/28
 */
public interface IProgressTable {

    int getProgressId();

    ProgressTypeEnum getProgressType();

    <T, E extends IProgressEvent<T>> IProgressCondition<T, E> getProgressCondition();

    <T> IReward<T> getReward();

    boolean isAutoGain();
}
