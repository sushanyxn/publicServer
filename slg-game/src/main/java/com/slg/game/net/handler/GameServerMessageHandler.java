package com.slg.game.net.handler;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.game.SpringContext;
import com.slg.net.message.clientmessage.login.packet.CM_LoginReq;
import com.slg.net.message.core.manager.MessageHandlerManager;
import com.slg.net.message.core.model.MessageHandlerMeta;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionRequest;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;

/**
 * 游戏服务端消息处理器
 * 负责接收网络消息
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
@Component("webSocketServerMessageHandler")
public class GameServerMessageHandler implements WebSocketMessageHandler {
    
    @Override
    public void onConnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.info("客户端连接建立，Session: {}", session);
    }
    
    @Override
    public void onMessage(ChannelHandlerContext ctx, Object message) {
        NetSession session = NetSession.getSession(ctx.channel());

        // 获取消息处理器元数据
        MessageHandlerMeta messageHandlerMeta = MessageHandlerManager.getInstance().getHandler(message.getClass());
        if (messageHandlerMeta == null) {
            LoggerUtil.error("消息{}未被注册！！", message.getClass().getSimpleName());
            return;
        }

        MethodHandle method = messageHandlerMeta.getMethodHandle();
        Object bean = messageHandlerMeta.getHandlerBean();

        // 根据连接类型和消息类型分发到不同的处理方法
        if (session.getPlayerId() > 0) {
            // 已注册玩家的消息
            GameHandlerUtil.handlePlayerMessage(session, message, method, bean);
        } else if (session.getServerId() > 0) {
            // 已注册服务器的消息（RPC 或内部协议）
            GameHandlerUtil.handleServerMessage(session, message, method, bean);
        } else if (message instanceof CM_LoginReq) {
            // 玩家登录消息
            GameHandlerUtil.handleLoginMessage(session, message, method, bean);
        } else if (message instanceof IM_RegisterSessionRequest) {
            // 内部连接注册消息
            GameHandlerUtil.handleRegisterMessage(session, message, method, bean);
        } else {
            LoggerUtil.error("未认证连接发送非法消息: {}, 远程地址: {}, 断开连接",
                    message.getClass().getSimpleName(), ctx.channel().remoteAddress());
            ctx.close();
        }
    }

    
    @Override
    public void onDisconnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        if (session != null) {
            LoggerUtil.info("客户端断开连接，Session: {}", session);
            if (session.getPlayerId() > 0){
                LoggerUtil.info("玩家 {} 断开连接", session.getPlayerId());
                Executor.Login.execute(() -> {
                    SpringContext.getLoginService().logout(session);
                });
            }else if (session.getServerId() > 0){
                LoggerUtil.info("服务器 {} 断开连接", session.getServerId());
            }
        }
    }
    
    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause) {
        if ("Connection reset".equals(cause.getMessage())) {
            return;
        }
        LoggerUtil.error("服务端连接异常: " + ctx.channel().id().asShortText(), cause);
    }
    
}

