package com.slg.common.progress.model;

import com.slg.common.event.model.IEvent;
import com.slg.common.progress.type.ProgressOwnerEnum;

/**
 * 进度事件接口
 * 所有进度相关的事件都应该实现此接口
 * 
 * @author yangxunan
 * @date 2026/1/28
 */
public interface IProgressEvent<T> extends IEvent {

    /**
     * 获取进度拥有者类型
     */
    ProgressOwnerEnum getOwnerEnum();

    /**
     * 获取进度拥有者ID
     */
    long getOwnerId();


}
