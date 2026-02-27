package com.slg.net.syncbus;

/**
 * 同步总线 缓存方
 *
 * @author yangxunan
 * @date 2026/2/12
 */
public interface ISyncCache {

    /**
     * 实体唯一标识（如 playerId）
     *
     * @return 同步 ID
     */
    long getSyncId();

    /**
     * 字段被同步更新后的回调（可选）
     * 子类可覆写此方法以响应同步事件
     *
     * @param fieldName 被更新的字段名
     * @param newValue  新值
     */
    default void onSyncUpdated(String fieldName, Object newValue) {
    }
}
