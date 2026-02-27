package com.slg.web.net.rpc.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.model.NetSession;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web 服内部会话管理
 * 管理 game 服主动连接到 web 服后的 session 映射
 * Web 端是被连接方，不需要重连逻辑
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class WebInnerSessionManager {

    @Getter
    private static WebInnerSessionManager instance;

    private final Map<Integer, NetSession> serverId2Session = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 根据 serverId 获取对应 game 服的连接
     *
     * @param serverId game 服 ID
     * @return 对应的 NetSession，不存在返回 null
     */
    public NetSession getSessionByServerId(int serverId) {
        return serverId2Session.get(serverId);
    }

    /**
     * 注册 game 服连接
     * 收到 IM_RegisterSessionRequest 后调用
     *
     * @param serverId game 服 ID
     * @param session  网络会话
     */
    public void registerSession(int serverId, NetSession session) {
        session.setServerId(serverId);
        serverId2Session.put(serverId, session);
        LoggerUtil.debug("[WebSession] GameServer {} 已注册连接", serverId);
    }

    /**
     * 移除断开的 game 服连接
     *
     * @param session 断开的网络会话
     */
    public void removeSession(NetSession session) {
        int serverId = session.getServerId();
        if (serverId2Session.remove(serverId, session)) {
            LoggerUtil.debug("[WebSession] GameServer {} 连接断开", serverId);
        }
    }

    /**
     * 获取所有已连接的 game 服 ID
     *
     * @return 不可修改的 serverId 集合
     */
    public Set<Integer> getConnectedServerIds() {
        return Collections.unmodifiableSet(serverId2Session.keySet());
    }
}
