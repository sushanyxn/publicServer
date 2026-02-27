package com.slg.net.rpc.handler;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.manager.MessageHandlerManager;
import com.slg.net.message.core.model.MessageHandlerMeta;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRespone;
import com.slg.net.rpc.util.RpcThreadUtil;
import com.slg.net.socket.model.NetSession;
import com.slg.net.socket.handler.WebSocketMessageHandler;
import io.netty.channel.ChannelHandlerContext;

import java.lang.invoke.MethodHandle;

/**
 * RPC 服务端消息处理器
 * 专门处理 RPC 服务器间的通信
 * 
 * @author yangxunan
 * @date 2026/01/26
 */
public class RpcServerMessageHandler implements WebSocketMessageHandler {

    /**
     * 内部连接断线监听器（由业务模块实现，可为 null）
     */
    private final InnerSessionDisconnectListener disconnectListener;

    /**
     * 无监听器构造（兼容无需断线回调的场景）
     */
    public RpcServerMessageHandler() {
        this(null);
    }

    /**
     * 带监听器构造
     *
     * @param disconnectListener 断线回调监听器，可为 null
     */
    public RpcServerMessageHandler(InnerSessionDisconnectListener disconnectListener) {
        this.disconnectListener = disconnectListener;
    }

    @Override
    public void onConnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.debug("[RPC Server] 服务器连接建立，Session: {}", session);
    }
    
    @Override
    public void onMessage(ChannelHandlerContext ctx, Object message) {
        NetSession session = NetSession.getSession(ctx.channel());

        // 获取消息处理器
        MessageHandlerMeta messageHandlerMeta = MessageHandlerManager.getInstance().getHandler(message.getClass());
        if (messageHandlerMeta == null) {
            LoggerUtil.error("[RPC Server] 消息 {} 未被注册！！", message.getClass().getSimpleName());
            return;
        }

        MethodHandle method = messageHandlerMeta.getMethodHandle();
        Object bean = messageHandlerMeta.getHandlerBean();

        // RPC 请求消息处理（通过 RpcThreadUtil.dispatch 分派到虚拟线程链）
        if (message instanceof IM_RpcRequest imRpcRequest) {
            RpcThreadUtil.dispatch(imRpcRequest, () -> {
                try {
                    method.invoke(bean, session, message);
                } catch (Throwable e) {
                    LoggerUtil.error("[RPC Server] RPC 请求处理异常", e);
                    throw new RuntimeException(e);
                }
            });
        }
        // RPC 响应消息处理（不需要线程切换，直接回调）
        else if (message instanceof IM_RpcRespone) {
            try {
                method.invoke(bean, session, message);
            } catch (Throwable e) {
                LoggerUtil.error("[RPC Server] RPC 响应处理异常", e);
                throw new RuntimeException(e);
            }
        }
        // 非 RPC 消息（如 IM_RegisterSessionRequest）：分发到 System 链执行
        // 保证与 removeSession、cancelPendingCleanup 等操作串行
        else {
            Executor.System.execute(() -> {
                try {
                    method.invoke(bean, session, message);
                } catch (Throwable e) {
                    LoggerUtil.error("[RPC Server] 内部协议处理异常: message={}",
                            message.getClass().getSimpleName(), e);
                    throw new RuntimeException(e);
                }
            });
        }
    }
    
    @Override
    public void onDisconnect(ChannelHandlerContext ctx) {
        NetSession session = NetSession.getSession(ctx.channel());
        LoggerUtil.debug("[RPC Server] 服务器断开连接，Session: {}", session);
        
        if (session != null && session.getServerId() > 0) {
            LoggerUtil.debug("[RPC Server] 服务器 {} 断开连接", session.getServerId());
            // 通知业务模块处理断线
            if (disconnectListener != null) {
                disconnectListener.onServerDisconnect(session);
            }
        }
    }
    
    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause) {
        if (cause.getMessage().equals("Connection reset")) {
            return;
        }
        LoggerUtil.error("[RPC Server] 连接异常: " + ctx.channel().id().asShortText(), cause);
    }
    
}
