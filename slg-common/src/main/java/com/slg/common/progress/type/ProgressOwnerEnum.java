package com.slg.common.progress.type;

import com.slg.common.event.model.IEvent;
import com.slg.common.progress.model.ProgressId;
import com.slg.common.progress.model.ProgressMeta;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进度拥有者类型
 * 每种类型的进度缓存独立
 *
 * @author yangxunan
 * @date 2026/1/28
 */
public enum ProgressOwnerEnum {

    // 个人进度
    Player,

    // 联盟进度
    Alliance,

    // 服务器进度
    Server,

    ;

    @Getter
    private final Map<Class<? extends IEvent>, Map<Long, Map<ProgressId, ProgressMeta>>>  event2Owner2ProgressMap = new ConcurrentHashMap<>();

}
