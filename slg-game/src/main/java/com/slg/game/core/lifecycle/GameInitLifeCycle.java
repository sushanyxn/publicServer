package com.slg.game.core.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.executor.Executor;
import com.slg.common.executor.GlobalScheduler;
import com.slg.common.executor.TaskModule;
import com.slg.common.log.LoggerUtil;
import com.slg.common.util.RandomUtil;
import com.slg.game.SpringContext;
import com.slg.game.base.player.manager.PlayerManager;
import com.slg.game.base.player.model.Player;
import com.slg.game.core.config.GameServerConfiguration;
import com.slg.game.net.manager.InnerSessionManager;
import com.slg.net.socket.client.WebSocketClientManager;
import com.slg.net.zookeeper.model.ServerType;
import com.slg.net.zookeeper.service.ZookeeperShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

/**
 * game业务生命周期
 * 负责初始化各种业务，包括启动场景服连接
 *
 * @author yangxunan
 * @date 2026/1/28
 */
@Component
public class GameInitLifeCycle implements SmartLifecycle {

    private volatile boolean running = false;

    @Autowired
    private PlayerManager playerManager;
    @Autowired
    private ZookeeperShareService shareService;
    @Autowired
    private GameServerConfiguration gameServerConfiguration;

    @Override
    public void start() {
        // 启动场景服连接轮询（副本由 startServerConnection 内部创建）
        InnerSessionManager.getInstance().startServerConnection(playerManager.getSceneServerContextMap());


        GlobalScheduler.getInstance().scheduleWithFixedDelay(TaskModule.SYSTEM, () -> {
            int random = RandomUtil.nextInt(10);
            for (Player player : SpringContext.getPlayerManager().getPlayers().values()) {
                if (player.getId() % 10 == random) {
                    Executor.Player.execute(player.getId(), () -> {
                        LoggerUtil.error("玩家{}出现异常错误！", player.getId(), new IllegalStateException());
                    });
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
        GlobalScheduler.getInstance().scheduleWithFixedDelay(TaskModule.SYSTEM, () -> {
            int random = RandomUtil.nextInt(10);
            for (Player player : SpringContext.getPlayerManager().getPlayers().values()) {
                if (random % 10 == random) {
                    Executor.Player.execute(player.getId(), () -> {
                        LoggerUtil.error("玩家{} error信息", player.getId());
                    });
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        // zk 实例
        shareService.createInstance(gameServerConfiguration.getServerId(), ServerType.GAME);

        running = true;
    }

    @Override
    public void stop() {
        LoggerUtil.debug("[GameInitLifeCycle] 开始停止，设置关闭标志并关闭内部连接");

        // 1. 设置关闭标志，防止断线触发重连逻辑（必须在关闭连接之前）
        InnerSessionManager.getInstance().shutdown();

        // 2. 主动关闭 Game→Scene 的 WebSocket 客户端连接（在执行器销毁前完成）
        //    断开连接会触发 onDisconnect → removeSession，但 shuttingDown 已设置，会跳过重连
        //    WebSocketClientManager 的 @PreDestroy 作为兜底，内部有 closed 标识防止重复执行
        WebSocketClientManager clientManager = WebSocketClientManager.getInstance();
        if (clientManager != null) {
            clientManager.shutdown();
        }

        // zk 实例
        shareService.destroyInstance(gameServerConfiguration.getServerId(), ServerType.GAME);
        running = false;
        LoggerUtil.debug("[GameInitLifeCycle] 停止完成");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.GAME_INIT;
    }
}
