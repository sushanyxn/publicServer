package com.slg.game.core.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.game.base.player.manager.PlayerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * 数据加载生命周期管理
 * 负责玩家数据加载，数据持久化由 CacheFlushLifeCycle 统一处理
 * 
 * @author yangxunan
 * @date 2026/01/28
 */
@Component
public class GameDataLoadingLifeCycle implements SmartLifecycle {
    
    @Autowired
    private PlayerManager playerManager;
    
    private volatile boolean running = false;

    @Override
    public void start() {
        playerManager.loadPlayers();
        running = true;
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
