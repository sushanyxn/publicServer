package com.slg.scene.net.handler;

import com.slg.common.log.LoggerUtil;
import com.slg.scene.net.manager.InnerSessionManager;
import com.slg.net.message.core.manager.MessageHandlerManager;
import com.slg.net.message.core.model.MessageHandlerMeta;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;

/**
 * 场景客户端消息处理器
 * 处理服务端发送的消息（SM_ 前缀）
 * 用于客户端测试或服务器间通信
 * 
 * @author yangxunan
 * @date 2026/02/02
 */
@Component("webSocketClientMessageHandler")
public class SceneClientMessageHandler implements WebSocketMessageHandler {
    
    @Override
    public void onConnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.info("场景服务器连接到服务器{}成功，Session: {}", session.getServerId(), session);

        // 注册内部连接
        InnerSessionManager.getInstance().registerSession(session.getServerId(), session);
    }
    
    @Override
    public void onMessage(ChannelHandlerContext ctx, Object message) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.debug("Session {} 收到消息: {}", session.getSessionId(), message.getClass().getSimpleName());
        
        try {
            if (session.getServerId() > 0) {
                // 获取消息处理器
                MessageHandlerMeta messageHandlerMeta = MessageHandlerManager.getInstance().getHandler(message.getClass());
                if (messageHandlerMeta == null) {
                    LoggerUtil.error("消息{}未被注册！！", message.getClass().getSimpleName());
                    return;
                }
                
                MethodHandle method = messageHandlerMeta.getMethodHandle();
                Object bean = messageHandlerMeta.getHandlerBean();
                // 已注册服务器的消息（RPC 或内部协议）
                SceneHandlerUtil.handleServerMessage(session, message, method, bean);
            } else {
                LoggerUtil.error("socket客户端收到未知服务器的消息{}", message.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LoggerUtil.error("客户端处理消息时发生异常", e);
        }
    }
    
    @Override
    public void onDisconnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.info("场景服务器与服务器{}断开连接，Session: {}", session.getServerId(), session);

        // 移除内部连接
        if (session.getServerId() > 0) {
            InnerSessionManager.getInstance().removeSession(session);
        }
    }
    
    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.error("与服务器{}连接异常: " + ctx.channel().id().asShortText(), session.getServerId(), cause);
    }
}
