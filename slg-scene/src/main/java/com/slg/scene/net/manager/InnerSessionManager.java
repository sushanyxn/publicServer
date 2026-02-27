package com.slg.scene.net.manager;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.model.NetSession;
import com.slg.scene.base.service.GameServerDisconnectHandler;
import com.slg.scene.core.config.SceneServerConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 内部会话管理器（Scene 侧）
 * 管理与其他服务器（如游戏服）的连接
 * 支持断线宽限期机制：断线后不立即清理，等待 Game 重连
 *
 * @author yangxunan
 * @date 2026/02/02
 */
@Component("sceneSessionManager")
public class InnerSessionManager {

    @Getter
    private static InnerSessionManager instance;

    @Autowired
    private SceneServerConfiguration sceneServerConfiguration;
    @Autowired
    private GameServerDisconnectHandler gameServerDisconnectHandler;

    /** 10秒宽限期（与 Game 侧 deadline 对齐） */
    private static final long GRACE_PERIOD_MS = 10000;

    /**
     * 服务器ID -> 会话映射
     */
    private Map<Integer, NetSession> serverId2Session = new ConcurrentHashMap<>();

    /**
     * 待清理定时任务：gameServerId → ScheduledFuture
     * 仅在 System 链中读写，无跨线程竞争
     */
    private final Map<Integer, ScheduledFuture<?>> pendingCleanupMap = new HashMap<>();

    /**
     * 进程正在关闭标志
     * 设为 true 后，removeSession 跳过宽限期定时器调度，仅做基本清理
     */
    private volatile boolean shuttingDown = false;

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 根据服务器ID获取会话
     *
     * @param serverId 服务器ID
     * @return 网络会话
     */
    public NetSession getSession(int serverId) {
        return serverId2Session.get(serverId);
    }

    /**
     * 根据服务器ID获取会话（兼容旧方法名）
     *
     * @param serverId 服务器ID
     * @return 网络会话
     */
    public NetSession getSessionByServerId(int serverId) {
        return serverId2Session.get(serverId);
    }

    /**
     * 设置关闭标志，防止断线触发宽限期清理逻辑
     * 由 SceneInitLifeCycle.stop() 调用
     */
    public void shutdown() {
        this.shuttingDown = true;
        LoggerUtil.debug("[Scene连接管理] 关闭标志已设置，后续断线将跳过宽限期逻辑");
    }

    /**
     * 注册服务器会话
     *
     * @param serverId 服务器ID
     * @param session  网络会话
     */
    public void registerSession(int serverId, NetSession session) {
        session.setServerId(serverId);
        serverId2Session.put(serverId, session);
    }

    /**
     * 断线处理（在 System 链中调用，由 SceneDisconnectListener 回调分发）
     * 不立即清理，而是启动宽限期（GRACE_PERIOD_MS）。如果 Game 在宽限期内重连，取消清理；超时后才正式清理。
     *
     * @param session 断开的会话
     */
    public void removeSession(NetSession session) {
        int gameServerId = session.getServerId();
        boolean remove = serverId2Session.remove(gameServerId, session);
        if (!remove) {
            return;
        }

        if (gameServerId <= 0) {
            return;
        }

        // 正在关闭，仅做基本清理，跳过宽限期定时器调度
        if (shuttingDown) {
            LoggerUtil.debug("[Scene连接管理] 进程正在关闭，Game服{}断线跳过宽限期", gameServerId);
            return;
        }

        LoggerUtil.debug("[Scene连接管理] Game服{}断开连接，启动{}ms宽限期", gameServerId, GRACE_PERIOD_MS);

        // 先取消旧定时器再 put 新定时器
        // 顺序不可颠倒：若先 put 后 cancel，旧定时器可能在 put 和 cancel 之间触发
        // 并执行 pendingCleanupMap.remove(gameServerId)，此时 remove 掉的是新定时器的引用
        ScheduledFuture<?> oldFuture = pendingCleanupMap.get(gameServerId);
        if (oldFuture != null && !oldFuture.isDone()) {
            oldFuture.cancel(false);
        }

        // 调度宽限期定时器（System 链中调度，定时器回调也在 System 链执行）
        ScheduledFuture<?> future = Executor.System.schedule(() -> {
            // 二次检查：如果宽限期内已重连，跳过清理（安全网）
            NetSession currentSession = serverId2Session.get(gameServerId);
            if (currentSession != null && currentSession.isActive()) {
                LoggerUtil.debug("[Scene连接管理] Game服{}在宽限期内已重连，跳过清理", gameServerId);
                pendingCleanupMap.remove(gameServerId);
                return;
            }
            pendingCleanupMap.remove(gameServerId);

            LoggerUtil.debug("[Scene连接管理] Game服{}宽限期到期，开始清理", gameServerId);

            // 实际节点清理分发到 Scene 链执行
            Executor.Scene.execute(() -> {
                gameServerDisconnectHandler.handleGameServerLost(gameServerId);
            });
        }, GRACE_PERIOD_MS, TimeUnit.MILLISECONDS);

        pendingCleanupMap.put(gameServerId, future);
    }

    /**
     * Game 重连注册时调用，取消宽限期定时器
     * 必须在 System 链中调用（由 handleRegisterMessage 保证）
     *
     * @param gameServerId 游戏服务器ID
     * @return true 表示在宽限期内重连（不需要重新初始化），false 表示已过宽限期或首次连接
     */
    public boolean cancelPendingCleanup(int gameServerId) {
        ScheduledFuture<?> future = pendingCleanupMap.remove(gameServerId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            LoggerUtil.debug("[Scene连接管理] Game服{}在宽限期内重连，取消清理定时器", gameServerId);
            return true; // 宽限期内重连，定时器尚未执行
        }
        return false; // 已过宽限期（定时器已执行完毕）或无待清理任务（首次连接）
    }

}
