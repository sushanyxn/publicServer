package com.slg.scene.core.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.scene.base.manager.ScenePlayerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * 场景数据加载生命周期
 * 负责场景数据加载，数据持久化由 CacheFlushLifeCycle 统一处理
 * 
 * @author yangxunan
 * @date 2026/02/02
 */
@Component
public class SceneDataLoadingLifeCycle implements SmartLifecycle {
    
    @Autowired
    private ScenePlayerManager scenePlayerManager;
    
    private volatile boolean running = false;

    @Override
    public void start() {
        LoggerUtil.debug("场景数据加载中...");
        scenePlayerManager.loadScenePlayers();
        running = true;
        LoggerUtil.debug("场景数据加载完成");
    }
    
    @Override
    public void stop() {
        running = false;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public int getPhase() {
        return LifecyclePhase.DATA_LOADING;
    }
}
