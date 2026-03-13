package com.slg.client.core.net;

import com.slg.client.SpringContext;
import com.slg.client.core.account.AccountManager;
import com.slg.client.core.account.ClientAccount;
import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.manager.MessageHandlerManager;
import com.slg.net.message.core.model.MessageHandlerMeta;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import com.slg.net.socket.model.NetSession;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;

/**
 * 客户端消息处理器
 * 将服务器响应路由到对应账号的 ClientHandler 进行处理
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component("webSocketClientMessageHandler")
public class ClientMessageHandler implements WebSocketMessageHandler {

    @Override
    public void onConnect(ChannelHandlerContext ctx) {
        LoggerUtil.debug("WebSocket 连接已建立: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, Object message) {
        MessageHandlerMeta meta = MessageHandlerManager.getInstance().getHandler(message.getClass());
        if (meta == null) {
            LoggerUtil.warn("未找到消息处理器: {}", message.getClass().getSimpleName());
            return;
        }

        MethodHandle methodHandle = meta.getMethodHandle();
        Object bean = meta.getHandlerBean();
        NetSession session = NetSession.getSession(ctx.channel());
        AccountManager accountManager = SpringContext.getAccountManager();
        ClientAccount account = accountManager.getBySession(session);

        if (account == null) {
            LoggerUtil.warn("未找到 session 对应的账号，消息被丢弃: {}", message.getClass().getSimpleName());
            return;
        }

        if (meta.isNeedOwner()) {
            Executor.Client.execute(account.getPlayerId(), () -> {
                try {
                    methodHandle.invoke(bean, session, message, account);
                } catch (Throwable e) {
                    LoggerUtil.error("处理消息异常: {}", message.getClass().getSimpleName(), e);
                }
            });
        } else {
            Executor.Client.execute(account.getPlayerId(), () -> {
                try {
                    methodHandle.invoke(bean, session, message);
                } catch (Throwable e) {
                    LoggerUtil.error("处理消息异常: {}", message.getClass().getSimpleName(), e);
                }
            });
        }
    }

    @Override
    public void onDisconnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        if (session != null) {
            AccountManager accountManager = SpringContext.getAccountManager();
            accountManager.onSessionClosed(session);
        }
        LoggerUtil.debug("WebSocket 连接已断开: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause) {
        LoggerUtil.error("WebSocket 连接异常: {}", ctx.channel().remoteAddress(), cause);
    }
}
