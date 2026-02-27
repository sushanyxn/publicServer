package com.slg.game.base.player.model;

import com.slg.game.SpringContext;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 服务器级别的场景服上下文
 * 管理与某个 Scene 服的连接状态和关联玩家
 *
 * @author yangxunan
 * @date 2026/2/11
 */
@Getter
@Setter
public class SceneServerContext {

    /**
     * 场景服连接状态枚举
     * 纯服务器连接状态，仅描述与 Scene 服的连接可用性
     * 不代表玩家场景已初始化——玩家初始化的唯一依据是 Player.sceneContext.sceneInit
     */
    public enum ConnectState {
        /** 未连接（初始状态 / 断线后） */
        DISCONNECTED,
        /** 连接中（正在尝试重连 / 已连接等待注册确认） */
        CONNECTING,
        /** 连接就绪，可用于 RPC 通信 */
        READY
    }

    /**
     * 场景服id
     */
    private int sceneServerId;

    /**
     * 关联的玩家id集合
     */
    private Set<Long> playerIds = ConcurrentHashMap.newKeySet();

    /**
     * 连接失败次数
     */
    private int connectFailCount = 0;

    /**
     * 连接状态（AtomicReference 保证状态转换的原子性）
     * 跨线程可见性：socketRegisterResponse、LoginService.login() 在其他线程读取
     */
    private final AtomicReference<ConnectState> connectState = new AtomicReference<>(ConnectState.DISCONNECTED);

    public static SceneServerContext valueOf(int sceneServerId) {
        SceneServerContext context = new SceneServerContext();
        context.sceneServerId = sceneServerId;
        return context;
    }

    public void addPlayer(long playerId) {
        playerIds.add(playerId);
    }

    public void removePlayer(long playerId) {
        playerIds.remove(playerId);
    }

    public void connectFail() {
        connectFailCount++;
    }

    public void connectSuccess() {
        connectFailCount = 0;
    }

    public void closeServer() {

    }

}
