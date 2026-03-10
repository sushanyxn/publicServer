package com.slg.net.socket.model;

import com.slg.common.log.LoggerUtil;
import com.slg.common.util.TimeUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;

/**
 * 网络会话
 * 封装客户端和服务端的连接，提供统一的消息发送接口
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
@Getter
@Setter
public class NetSession {
    
    /**
     * 用于在 Channel 中存储 Session 的 Key
     */
    public static final AttributeKey<NetSession> SESSION_KEY = AttributeKey.valueOf("NET_SESSION");
    
    /**
     * 网络通道
     */
    private final Channel channel;
    
    /**
     * 会话ID（Channel ID）
     */
    private final String sessionId;
    
    /**
     * 创建时间
     */
    private final long createTime;
    
    /**
     * 是否已认证
     */
    private volatile boolean authenticated = false;
    
    /**
     * 玩家ID（登录后设置）表示这是一个玩家连接
     */
    private volatile long playerId;

    /**
     * 服务器ID（注册后设置）表示这是一个内部连接
     */
    private volatile int serverId;
    
    /**
     * 构造函数
     */
    public NetSession(Channel channel) {
        this.channel = channel;
        this.sessionId = channel.id().asShortText();
        this.createTime = System.currentTimeMillis();
        
        // 将 Session 存储到 Channel 的 Attribute 中
        channel.attr(SESSION_KEY).set(this);
        
        LoggerUtil.debug("NetSession 创建: {}", sessionId);
    }
    
    /**
     * 从 Channel 中获取 Session
     */
    public static NetSession getSession(Channel channel) {
        if (channel == null) {
            return null;
        }
        return channel.attr(SESSION_KEY).get();
    }
    
    /**
     * 发送消息
     */
    public ChannelFuture sendMessage(Object message) {
        if (!isActive()) {
            LoggerUtil.warn("Session {} 未激活，无法发送消息", sessionId);
            return null;
        }
        
        return channel.writeAndFlush(message);
    }

    /**
     * 获取连接类型描述（用于日志）
     * @return 玩家连接 / 服务器连接 / 未注册连接
     */
    private String getConnectionTypeDesc() {
        if (playerId > 0) {
            return "玩家连接(playerId=" + playerId + ")";
        }
        if (serverId != 0) {
            return "服务器连接(serverId=" + serverId + ")";
        }
        return "未注册连接";
    }

    /**
     * 关闭会话
     */
    public void close(String reason) {
        if (channel != null && channel.isActive()) {
            LoggerUtil.info("关闭 Session: {}, 类型: {}, 原因: {}", sessionId, getConnectionTypeDesc(), reason);
            channel.close();
        }
    }
    
    /**
     * 会话是否激活
     */
    public boolean isActive() {
        return channel != null && channel.isActive();
    }
    
    /**
     * 获取远程地址
     */
    public String getRemoteAddress() {
        if (channel == null || channel.remoteAddress() == null) {
            return "unknown";
        }
        return channel.remoteAddress().toString();
    }
    
    /**
     * 获取会话存活时间（毫秒）
     */
    public long getAliveTime() {
        return TimeUtil.now() - createTime;
    }
    
    /**
     * 设置玩家ID
     */
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
        if (playerId > 0){
            LoggerUtil.info("Session {} 绑定玩家: {}", sessionId, playerId);
        }
    }
    
    /**
     * 设置服务器ID
     */
    public void setServerId(int serverId) {
        this.serverId = serverId;
        LoggerUtil.info("Session {} 绑定服务器: {}", sessionId, serverId);
    }

    @Override
    public String toString() {
        return "NetSession{" +
                "sessionId='" + sessionId + '\'' +
                ", playerId=" + playerId +
                ", serverId=" + serverId +
                ", authenticated=" + authenticated +
                ", active=" + isActive() +
                ", remoteAddress='" + getRemoteAddress() + '\'' +
                '}';
    }
    
    /**
     * Channel Future 监听器接口
     */
    @FunctionalInterface
    public interface ChannelFutureListener {
        void operationComplete(ChannelFuture future);
    }
}

