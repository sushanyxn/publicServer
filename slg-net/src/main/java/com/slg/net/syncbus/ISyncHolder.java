package com.slg.net.syncbus;

/**
 * 同步总线 发起方
 *
 * @author yangxunan
 * @date 2026/2/12
 */
public interface ISyncHolder {

    /**
     * 实体唯一标识（如 playerId）
     *
     * @return 同步 ID
     */
    long getSyncId();

    /**
     * 需要同步的目标服务器 ID 列表
     *
     * @return 目标服务器 ID 数组
     */
    int[] syncTargetServerIds();

    /**
     * 同步对应字段到远端
     *
     * @param fieldNames 要同步的字段名列表
     */
    default void sync(String... fieldNames){
        SyncBus.sync(this, fieldNames);
    }

    /**
     * 同步所有字段
     */
    default void syncAll(){
        SyncBus.syncAll(this);
    }
}
