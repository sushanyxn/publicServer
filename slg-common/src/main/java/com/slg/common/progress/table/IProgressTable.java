package com.slg.common.progress.table;

import com.slg.common.bean.IReward;
import com.slg.common.progress.bean.IProgressCondition;
import com.slg.common.progress.model.IProgressEvent;
import com.slg.common.progress.type.ProgressTypeEnum;

/**
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
