package com.slg.net.socket.server;

import com.slg.common.log.LoggerUtil;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import com.slg.net.socket.manager.WebSocketConnectionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * WebSocket 服务端业务处理器
 *
 * @author yangxunan
 * @date 2025-12-25
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketMessageHandler messageHandler;
    private final WebSocketConnectionManager connectionManager;

    public WebSocketServerHandler(
            WebSocketMessageHandler messageHandler,
            WebSocketConnectionManager connectionManager) {
        this.messageHandler = messageHandler;
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 创建 NetSession
        NetSession session = new NetSession(ctx.channel());
        
        connectionManager.addChannel(ctx.channel());
        messageHandler.onConnect(ctx);
        
        LoggerUtil.info("服务端连接建立，Session: {}", session.getSessionId());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object message) {
        // 过滤 WebSocket 握手期间的 HTTP 消息
        // 这些消息应该由 WebSocketServerProtocolHandler 处理，但有时会泄漏到业务层
        if (message instanceof io.netty.handler.codec.http.HttpObject) {
            LoggerUtil.debug("忽略 WebSocket 握手消息: {}", message.getClass().getSimpleName());
            return;
        }
        
        // 只处理解码后的业务消息
        messageHandler.onMessage(ctx, message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionManager.removeChannel(ctx.channel());
        messageHandler.onDisconnect(ctx);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.READER_IDLE || idleEvent.state() == IdleState.ALL_IDLE) {
                LoggerUtil.warn("WebSocket 连接空闲超时({}), 关闭连接: {}",
                        idleEvent.state(), ctx.channel().id().asShortText());

                NetSession session = NetSession.getSession(ctx.channel());
                if (session != null) {
                    session.close("链接空闲超时");
                }else {
                    ctx.close();
                }
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        messageHandler.onError(ctx, cause);
        ctx.close();
    }
}
