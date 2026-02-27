package com.slg.game.core.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.game.net.manager.PlayerSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * 会话管理生命周期
 * 负责管理玩家会话
 * 
 * @author yangxunan
 * @date 2026/01/28
 */
@Component
public class SessionManagementLifeCycle implements SmartLifecycle {
    
    @Autowired
    private PlayerSessionManager playerSessionManager;
    
    private volatile boolean running = false;
    
    @Override
    public void start() {
        // 会话管理器通常不需要特殊的启动逻辑
        running = true;
    }
    
    @Override
    public void stop() {
        // 关闭所有连接
        playerSessionManager.shutdown();

        running = false;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public int getPhase() {
        // 与 WebSocket 服务器同级，确保在 WebSocket 关闭时一起关闭
        return LifecyclePhase.WEBSOCKET_SERVER;
    }
}
