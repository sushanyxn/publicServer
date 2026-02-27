package com.slg.common.progress.bean;

import com.slg.common.bean.ICondition;
import com.slg.common.progress.model.IProgressEvent;
import com.slg.common.progress.model.ProgressMeta;

/**
 * @author yangxunan
 * @date 2026/1/28
 */
public interface IProgressCondition<T, E extends IProgressEvent<T>> extends ICondition<T> {

    /**
     * 初始化进度
     * @param owner 进度拥有者
     * @param meta 进度元数据
     */
    void init(T owner, ProgressMeta meta);

    /**
     * 进度事件监听
     * @param event 事件
     * @param meta 进度元数据
     */
    void onEvent(E event, ProgressMeta meta);


    /**
     * @return 完成的进度值
     */
    long getFinishProgress();

}
