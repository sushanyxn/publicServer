package com.slg.game.net.handler;

import com.slg.common.log.LoggerUtil;
import com.slg.common.executor.Executor;
import com.slg.game.net.manager.InnerSessionManager;
import com.slg.net.message.core.manager.MessageHandlerManager;
import com.slg.net.message.core.model.MessageHandlerMeta;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;

/**
 * 游戏客户端消息处理器
 * 处理 Game 作为 WebSocket 客户端连接 Scene RPC 服务端时的消息
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
@Component("webSocketClientMessageHandler")
public class GameClientMessageHandler implements WebSocketMessageHandler {
    
    @Override
    public void onConnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        // 仅打印日志，不调用 registerSession
        // 原因：onConnect 在 IO 线程中触发，此时 connectServer 中的 setServerId 尚未执行，
        // session.getServerId() 返回 0，会导致 serverId2Session.put(0, session) 产生泄漏条目
        // connectServer 已负责正确注册 session（先 setServerId 再 put）
        LoggerUtil.debug("客户端与远程地址 {} 建立连接", ctx.channel().remoteAddress());
    }
    
    @Override
    public void onMessage(ChannelHandlerContext ctx, Object message) {
        NetSession session = NetSession.getSession(ctx.channel());

        try {
            if (session.getServerId() > 0){

                // 在此处实现消息分发逻辑
                MessageHandlerMeta messageHandlerMeta = MessageHandlerManager.getInstance().getHandler(message.getClass());
                if (messageHandlerMeta == null) {
                    LoggerUtil.error("消息{}未被注册！！", message.getClass().getSimpleName());
                    return;
                }
                MethodHandle method = messageHandlerMeta.getMethodHandle();
                Object bean = messageHandlerMeta.getHandlerBean();
                GameHandlerUtil.handleServerMessage(session, message, method, bean);
            }else {
                LoggerUtil.error("socket客户端收到未知服务器的消息{}", message.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LoggerUtil.error("客户端处理消息时发生异常", e);
        }
    }
    
    @Override
    public void onDisconnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.debug("客户端与服务器{}断开连接，Session: {}", session.getServerId(), session);

        // 将 removeSession 分发到 System 链执行，与 quickReconnect 的递归调度串行
        if (session.getServerId() > 0) {
            Executor.System.execute(() -> {
                InnerSessionManager.getInstance().removeSession(session);
            });
        }
    }
    
    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.error("与服务器{}连接异常: " + ctx.channel().id().asShortText(), session.getServerId(), cause);
    }
    
}
