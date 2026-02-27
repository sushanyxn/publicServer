package com.slg.net.syncbus;

/**
 * 缓存实体查找器接口
 * 业务模块实现此接口，每种 Cache 实体类型实现一个
 * 用于在接收端根据 syncId 查找对应的 Cache 实体
 *
 * @param <T> Cache 实体类型
 * @author yangxunan
 * @date 2026/02/12
 */
public interface ISyncCacheResolver<T extends ISyncCache> {

    /**
     * 对应的同步模块
     *
     * @return SyncModule 枚举值
     */
    SyncModule getSyncModule();

    /**
     * 根据同步 ID 查找 Cache 实体
     *
     * @param syncId 实体唯一标识
     * @return Cache 实体实例，不存在时返回 null
     */
    T resolve(long syncId);
}
