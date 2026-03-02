package com.slg.game.net.manager;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.game.base.login.service.LoginService;
import com.slg.game.base.player.manager.PlayerManager;
import com.slg.game.base.player.model.Player;
import com.slg.game.base.player.model.SceneServerContext;
import com.slg.game.base.player.model.SceneServerContext.ConnectState;
import com.slg.game.core.config.GameServerConfiguration;
import com.slg.game.core.lifecycle.GameInitLifeCycle;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionRequest;
import com.slg.net.rpc.exception.RpcDisconnectException;
import com.slg.net.rpc.manager.RpcCallBackManager;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.client.WebSocketClientManager;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 系统级别的连接管理
 * 管理 Game 进程到各个 Scene 服的 WebSocket 连接
 *
 * @author yangxunan
 * @date 2026/1/26
 */
@Component("gameSessionManager")
public class InnerSessionManager {

    @Getter
    private static InnerSessionManager instance;

    @Autowired
    private GameServerConfiguration gameserverConfiguration;
    @Autowired
    private PlayerManager playerManager;
    @Autowired
    private RpcCallBackManager rpcCallBackManager;
    @Autowired
    private LoginService loginService;
    @Autowired
    private GameInitLifeCycle gameInitLifeCycle;

    /**
     * 服务器id -> 连接
     */
    private Map<Integer, NetSession> serverId2Session = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 连接锁（替代 synchronized，符合虚拟线程规范）
     */
    private final ReentrantLock lock = new ReentrantLock();

    /** WebSocket 连接超时（毫秒）：快速重连在 System 单链中同步执行，超时越短阻塞越少 */
    private static final int CONNECT_TIMEOUT_MS = 1000;

    /** 注册响应超时（毫秒）：connectServer 成功后等待注册响应的最大时间 */
    private static final int REGISTER_TIMEOUT_MS = 5000;

    /** 快速重连次数 */
    private static final int QUICK_RECONNECT_ATTEMPTS = 3;

    /** 慢重连间隔（毫秒） */
    private static final long SLOW_RECONNECT_INTERVAL_MS = 10000;

    /** 断线 deadline 总时间（毫秒）：从断线到确认失联的最大等待时间，与 Scene 宽限期对齐 */
    private static final long DISCONNECT_DEADLINE_MS = 10000;

    /**
     * 进程正在关闭标志
     * 设为 true 后，removeSession 跳过重连和踢人逻辑，仅做基本清理
     */
    private volatile boolean shuttingDown = false;

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 设置关闭标志，防止断线触发重连逻辑
     * 由 GameInitLifeCycle.stop() 调用
     */
    public void shutdown() {
        this.shuttingDown = true;
        LoggerUtil.debug("[连接管理] 关闭标志已设置，后续断线将跳过重连逻辑");
    }

    public NetSession getSessionByServerId(int serverId) {
        return serverId2Session.get(serverId);
    }

    public void registerSession(int serverId, NetSession session) {
        session.setServerId(serverId);
        serverId2Session.put(serverId, session);
    }

    /**
     * 断线处理（在 System 链中调用）
     * 由 GameClientMessageHandler.onDisconnect 分发到 System 链执行
     */
    public void removeSession(NetSession session) {
        int sceneServerId = session.getServerId();
        boolean remove = serverId2Session.remove(sceneServerId, session);
        if (!remove) {
            return;
        }

        SceneServerContext ctx = playerManager.getSceneServerContextMap().get(sceneServerId);
        if (ctx == null) {
            // 没有场景上下文的，一般是普通链接 有可能是game->game，game->center，不需要做异常处理
            return;
        }

        // 断线时主动 fail 该连接上所有 pending RPC 回调（避免调用方等 30 秒超时）
        rpcCallBackManager.failAllByServerId(sceneServerId,
                new RpcDisconnectException("Scene server " + sceneServerId + " disconnected"));

        // 正在关闭，仅做基本清理，跳过重连和踢人
        if (shuttingDown) {
            ctx.getConnectState().set(ConnectState.DISCONNECTED);
            LoggerUtil.debug("[连接管理] 进程正在关闭，Scene服{}断线跳过重连", sceneServerId);
            return;
        }

        // 先设 DISCONNECTED：断线了状态就是断线，确保从任何状态（包括 CONNECTING）都能正确恢复
        ctx.getConnectState().set(ConnectState.DISCONNECTED);

        // CAS 防御：DISCONNECTED→CONNECTING（在 System 单链中此 CAS 必定成功，作为防御性编程保留）
        if (!ctx.getConnectState().compareAndSet(ConnectState.DISCONNECTED, ConnectState.CONNECTING)) {
            return;
        }

        // 启动快速重连（3次，无间隔，立即重试）+ deadline 兜底
        quickReconnect(sceneServerId, ctx);
    }

    /**
     * 快速重连 + deadline 机制
     * 先同步尝试 3 次（无间隔，最多阻塞 ~3 秒），失败后进入 deadline 阶段（异步调度，每 1 秒重试一次）。
     * deadline 从断线时刻开始计算，总时间 DISCONNECT_DEADLINE_MS（10 秒），与 Scene 宽限期对齐。
     * deadline 到期仍未成功才确认失联、踢人。
     */
    private void quickReconnect(int sceneServerId, SceneServerContext ctx) {
        // deadline 从断线时刻开始计算
        long deadlineEnd = System.currentTimeMillis() + DISCONNECT_DEADLINE_MS;

        for (int attempt = 0; attempt < QUICK_RECONNECT_ATTEMPTS; attempt++) {
            if (connectServer(sceneServerId)) {
                // 重连成功，后续由 socketRegisterResponse 根据 needReInit 决定是否初始化
                return;
            }
        }
        // 快速重连全部失败，进入 deadline 阶段（每 1 秒异步重试一次，不阻塞 System 链）
        LoggerUtil.debug("[连接管理] Scene服{}快速重连失败，进入deadline阶段（剩余{}ms）",
                sceneServerId, deadlineEnd - System.currentTimeMillis());
        deadlineReconnect(sceneServerId, ctx, deadlineEnd);
    }

    /**
     * deadline 阶段重连：每 1 秒通过 schedule 异步尝试一次，不阻塞 System 链
     * deadline 到期仍未成功则确认失联，执行 handleSceneServerLost
     */
    private void deadlineReconnect(int sceneServerId, SceneServerContext ctx, long deadlineEnd) {
        Executor.System.schedule(() -> {
            // 进程正在关闭，停止重连
            if (shuttingDown) {
                return;
            }
            // deadline 到期，确认失联
            if (System.currentTimeMillis() >= deadlineEnd) {
                handleSceneServerLost(sceneServerId, ctx);
                return;
            }
            // 尝试重连
            if (!connectServer(sceneServerId)) {
                // 还没到 deadline，继续调度
                deadlineReconnect(sceneServerId, ctx, deadlineEnd);
            }
            // 重连成功，后续由 socketRegisterResponse 处理
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Scene 服确认失联：标记玩家 sceneInit=false，踢在线玩家下线，启动慢重连
     */
    private void handleSceneServerLost(int sceneServerId, SceneServerContext ctx) {
        LoggerUtil.error("[连接管理] Scene服{}确认失联，开始踢玩家下线并启动慢重连", sceneServerId);

        // 遍历受影响的玩家，在 Player 链中先设标记再条件踢人
        for (long playerId : ctx.getPlayerIds()) {
            Executor.Player.execute(playerId, () -> {
                Player player = playerManager.getPlayer(playerId);
                if (player == null) {
                    return;
                }
                // 先设标记：保证玩家重登时一定能看到 sceneInit=false
                player.getSceneContext().setSceneInit(false);
                // 再条件踢人：只踢在线玩家
                if (player.getSession() != null && player.getSession().isActive()) {
                    Executor.Login.execute(() -> loginService.logout(player.getSession()));
                }
            });
        }

        // 启动慢重连（延迟后首次尝试，确保 Scene 宽限期已过，避免窗口竞态）
        Executor.System.schedule(() -> {
            startConnection(ctx, SLOW_RECONNECT_INTERVAL_MS);
        }, SLOW_RECONNECT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 连接到指定的场景服
     * 修复项：synchronized→ReentrantLock、返回值修复、WebSocket连接超时1秒、
     * 连接成功后设CONNECTING、注册响应超时检测
     *
     * @param serverId 目标服务器ID
     * @return true 表示连接成功（注册尚未确认），false 表示连接失败
     */
    public boolean connectServer(int serverId) {

        if (shuttingDown) {
            return false;
        }

        if (serverId == gameserverConfiguration.getServerId()) {
            return true; // 本服直接返回
        }

        NetSession netSession = serverId2Session.get(serverId);
        if (netSession != null && netSession.isActive()) {
            return true; // 已有活跃连接
        }

        lock.lock();
        try {
            netSession = serverId2Session.get(serverId);
            if (netSession != null && netSession.isActive()) {
                return true;
            }

            String url = serverId == gameserverConfiguration.getBindSceneId() ? gameserverConfiguration.getBindSceneUrl() : "url从zk中获取";
            // 连接超时 1 秒：快速重连 3 次最多阻塞 System 链约 3 秒
            netSession = WebSocketClientManager.getInstance().connect(url, CONNECT_TIMEOUT_MS);
            if (netSession != null && netSession.isActive()) {
                netSession.setServerId(serverId);
                serverId2Session.put(serverId, netSession);

                // 设置 CONNECTING：确保首次启动和断线重连场景下注册超时检测都能生效
                // 断线重连时 removeSession 已设了 CONNECTING，此处再设是幂等操作
                SceneServerContext ctx = playerManager.getSceneServerContextMap().get(serverId);
                if (ctx != null) {
                    ctx.getConnectState().set(ConnectState.CONNECTING);
                }

                // 发送注册消息
                netSession.sendMessage(IM_RegisterSessionRequest.valueOf(gameserverConfiguration.getServerId()));

                // 调度注册响应超时检查
                final NetSession sessionRef = netSession;
                Executor.System.schedule(() -> {
                    SceneServerContext timeoutCtx = playerManager.getSceneServerContextMap().get(serverId);
                    if (timeoutCtx != null && timeoutCtx.getConnectState().get() == ConnectState.CONNECTING) {
                        // 注册响应超时：ConnectState 仍为 CONNECTING，主动断开触发重连
                        LoggerUtil.error("[连接管理] Scene服{}注册响应超时({}ms)，主动断开触发重连",
                                serverId, REGISTER_TIMEOUT_MS);
                        sessionRef.close("注册响应超时");
                    }
                }, REGISTER_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                return true; // 连接成功（注册尚未确认，由 socketRegisterResponse 设 READY）
            }
            return false; // 连接失败
        } finally {
            lock.unlock();
        }
    }

    /**
     * 启动场景服连接轮询（入口方法）
     * 内部创建工作副本，避免 remove 操作影响原始 sceneServerContextMap
     *
     * @param originalContextMap 原始的场景服上下文映射
     */
    public void startServerConnection(Map<Integer, SceneServerContext> originalContextMap) {
        Map<Integer, SceneServerContext> contextMap = new HashMap<>(originalContextMap);
        doStartServerConnection(contextMap);
    }

    /**
     * 实际的连接轮询逻辑（递归调度）
     * 使用单次 schedule + 递归模式，避免 scheduleAtFixedRate + 递归调用叠加的 bug
     */
    private void doStartServerConnection(Map<Integer, SceneServerContext> contextMap) {
        Iterator<Map.Entry<Integer, SceneServerContext>> it = contextMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, SceneServerContext> entry = it.next();
            if (connectServer(entry.getKey())) {
                entry.getValue().connectSuccess();
                it.remove();
            } else {
                entry.getValue().connectFail();
                LoggerUtil.debug("正在等待服务器{}上线, 已重试{}次",
                        entry.getKey(), entry.getValue().getConnectFailCount());
            }
        }

        // 还有未连接的服务器，延迟后递归调度下一轮
        if (!contextMap.isEmpty()) {
            Executor.System.schedule(() -> {
                doStartServerConnection(contextMap);
            }, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * 单独连接某个服务器（慢重连，用于 Scene 失联后的持续探测）
     *
     * @param sceneServerContext 场景服上下文
     * @param time               循环连接间隔（毫秒）
     */
    public void startConnection(SceneServerContext sceneServerContext, long time) {
        if (!connectServer(sceneServerContext.getSceneServerId())) {
            Executor.System.schedule(() -> {
                if (shuttingDown) {
                    return;
                }
                startConnection(sceneServerContext, time);
            }, time, TimeUnit.MILLISECONDS);
        }
    }

}
