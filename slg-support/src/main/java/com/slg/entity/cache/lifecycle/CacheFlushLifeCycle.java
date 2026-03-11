package com.slg.entity.cache.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.executor.core.ExecutorConstants;
import com.slg.common.executor.core.KeyedVirtualExecutor;
import com.slg.common.log.LoggerUtil;
import com.slg.entity.cache.manager.EntityCacheManager;
import com.slg.entity.cache.model.EntityCache;
import com.slg.entity.db.entity.BaseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * 缓存刷盘生命周期管理
 * 负责关停时统一保存所有缓存实体到数据库
 * 与具体数据库实现无关，通过 BaseEntity.save() 多态调用
 *
 * @author yangxunan
 * @date 2026/02/24
 */
@Component
public class CacheFlushLifeCycle implements SmartLifecycle {

    @Autowired
    private EntityCacheManager entityCacheManager;

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        try {
            LoggerUtil.debug("开始保存所有缓存数据");
            for (EntityCache<?> entityCache : entityCacheManager.getAllCaches().values()) {
                for (BaseEntity<?> entity : entityCache.getAllCache()) {
                    entity.save();
                }
                entityCache.flush();
            }
            LoggerUtil.debug("所有缓存数据已保存");

            LoggerUtil.debug("等待所有数据落地任务完成");
            KeyedVirtualExecutor.getInstance().awaitAllTasksComplete(ExecutorConstants.SHUTDOWN_TIMEOUT_MS);
            LoggerUtil.debug("所有数据落地任务已完成");
        } catch (Exception e) {
            LoggerUtil.error("数据保存过程中发生异常", e);
            throw new RuntimeException("数据保存失败", e);
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.DATABASE;
    }
}
