package com.slg.net.socket.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.model.NetSession;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 连接管理器
 * 管理所有活跃的 NetSession（纯支撑类，不包含业务逻辑）
 *
 * @author yangxunan
 * @date 2025-12-25
 */
public class WebSocketConnectionManager {

    /**
     * 存储所有会话
     * Key: SessionId, Value: NetSession
     */
    private final Map<String, NetSession> sessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(NetSession session) {
        sessions.put(session.getSessionId(), session);
        LoggerUtil.info("新增会话: {}, 当前会话数: {}", 
            session.getSessionId(), sessions.size());
    }
    
    /**
     * 添加连接（兼容旧接口）
     */
    public void addChannel(Channel channel) {
        NetSession session = NetSession.getSession(channel);
        if (session != null) {
            addSession(session);
        }
    }

    /**
     * 移除会话
     */
    public void removeSession(NetSession session) {
        if (session == null) {
            return;
        }
        
        sessions.remove(session.getSessionId());
        
        LoggerUtil.info("移除会话: {}, 当前会话数: {}", 
            session.getSessionId(), sessions.size());
    }
    
    /**
     * 移除连接（兼容旧接口）
     */
    public void removeChannel(Channel channel) {
        NetSession session = NetSession.getSession(channel);
        if (session != null) {
            removeSession(session);
        }
    }

    /**
     * 根据会话ID获取会话
     */
    public NetSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 获取所有会话
     */
    public Collection<NetSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * 获取连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }


    /**
     * 关闭所有连接
     */
    public void closeAll() {
        LoggerUtil.info("关闭所有会话，当前会话数: {}", sessions.size());
        for (NetSession session : sessions.values()) {
            session.close("服务器关闭");
        }
        sessions.clear();
    }
}
