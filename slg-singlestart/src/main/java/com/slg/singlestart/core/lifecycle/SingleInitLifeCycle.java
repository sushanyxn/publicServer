package com.slg.singlestart.core.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.game.SpringContext;
import com.slg.game.base.player.model.SceneServerContext;
import com.slg.game.base.player.service.PlayerService;
import com.slg.game.core.config.GameServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * 合并服业务初始化生命周期
 * 负责初始化合并服相关业务
 *
 * @author yangxunan
 * @date 2026/02/02
 */
@Component
public class SingleInitLifeCycle implements SmartLifecycle {

    private volatile boolean running = false;

    @Autowired
    private GameServerConfiguration gameServerConfiguration;
    @Autowired
    private PlayerService playerService;

    @Override
    public void start() {
        LoggerUtil.debug("合并服业务初始化");
        SceneServerContext sceneServerContext = SpringContext.getPlayerManager().getSceneServerContextMap().get(gameServerConfiguration.getBindSceneId());
        sceneServerContext.getConnectState().set(SceneServerContext.ConnectState.READY);
        Executor.System.execute(() -> {
            playerService.batchInitPlayerScene(gameServerConfiguration.getBindSceneId());
        });

        running = true;
        LoggerUtil.debug("合并服业务初始化完成");
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
        return LifecyclePhase.SINGLE_INIT;
    }
}
