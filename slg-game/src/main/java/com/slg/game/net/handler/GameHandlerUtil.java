package com.slg.game.net.handler;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.game.SpringContext;
import com.slg.game.base.player.model.Player;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRespone;
import com.slg.net.rpc.util.RpcThreadUtil;
import com.slg.net.socket.model.NetSession;

import java.lang.invoke.MethodHandle;

/**
 * @author yangxunan
 * @date 2026/2/2
 */
public class GameHandlerUtil {

    /**
     * 处理已注册玩家的消息
     * 在玩家线程中执行
     */
    public static void handlePlayerMessage(NetSession session, Object message, MethodHandle method, Object bean) {
        Player player = SpringContext.getPlayerManager().getPlayers().get(session.getPlayerId());
        if (player != null) {
            // 分发到玩家线程执行
            Executor.Player.execute(session.getPlayerId(), () -> {
                try {
                    method.invoke(bean, session, message, player);
                } catch (Throwable e) {
                    LoggerUtil.error("玩家消息处理异常: playerId={}, message={}",
                            session.getPlayerId(), message.getClass().getSimpleName(), e);
                    throw new RuntimeException(e);
                }
            });
        } else {
            LoggerUtil.error("{}协议接收时，找不到玩家{}",
                    message.getClass().getSimpleName(), session.getPlayerId());
        }
    }

    /**
     * 处理服务器间的消息
     * 包括 RPC 请求、RPC 响应和其他内部协议
     */
    public static void handleServerMessage(NetSession session, Object message, MethodHandle method, Object bean) {
        if (message instanceof IM_RpcRequest imRpcRequest) {
            // RPC 请求消息：需要线程切换到指定的 RPC 线程
            handleRpcRequest(session, message, method, bean, imRpcRequest);
        } else if (message instanceof IM_RpcRespone) {
            // RPC 响应消息：不需要线程切换，直接回调 Future
            handleRpcResponse(session, message, method, bean);
        } else {
            // 其他内部协议：在系统线程中执行
            handleInternalMessage(session, message, method, bean);
        }
    }

    /**
     * 处理 RPC 请求消息
     * 通过 RpcThreadUtil.dispatch 直接分派到对应的虚拟线程链执行
     */
    public static void handleRpcRequest(NetSession session, Object message, MethodHandle method,
                                  Object bean, IM_RpcRequest imRpcRequest) {
        RpcThreadUtil.dispatch(imRpcRequest, () -> {
            try {
                method.invoke(bean, session, message);
            } catch (Throwable e) {
                LoggerUtil.error("RPC 请求处理异常: method={}",
                        imRpcRequest.getMethodMarker(), e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 处理 RPC 响应消息
     * 分发到 RpcResponse 单链执行，避免在 Netty IO 线程中处理回调
     */
    public static void handleRpcResponse(NetSession session, Object message, MethodHandle method, Object bean) {
        Executor.RpcResponse.execute(() -> {
            try {
                method.invoke(bean, session, message);
            } catch (Throwable e) {
                LoggerUtil.error("RPC 响应处理异常", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 处理其他内部协议消息
     * 在系统线程中执行
     */
    public static void handleInternalMessage(NetSession session, Object message, MethodHandle method, Object bean) {
        Executor.System.execute(() -> {
            try {
                method.invoke(bean, session, message);
            } catch (Throwable e) {
                LoggerUtil.error("内部协议处理异常: message={}",
                        message.getClass().getSimpleName(), e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 处理玩家登录消息
     * 在登录线程中执行
     */
    public static void handleLoginMessage(NetSession session, Object message, MethodHandle method, Object bean) {
        Executor.Login.execute(() -> {
            try {
                method.invoke(bean, session, message);
            } catch (Throwable e) {
                LoggerUtil.error("登录消息处理异常", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 处理内部连接注册消息
     * 在系统线程中执行
     */
    public static void handleRegisterMessage(NetSession session, Object message, MethodHandle method, Object bean) {
        Executor.System.execute(() -> {
            try {
                method.invoke(bean, session, message);
            } catch (Throwable e) {
                LoggerUtil.error("连接注册消息处理异常", e);
                throw new RuntimeException(e);
            }
        });
    }
    
}
