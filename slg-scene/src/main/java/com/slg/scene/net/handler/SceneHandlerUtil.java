package com.slg.scene.net.handler;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRespone;
import com.slg.net.rpc.util.RpcThreadUtil;
import com.slg.net.socket.model.NetSession;

import java.lang.invoke.MethodHandle;

/**
 * @author yangxunan
 * @date 2026/2/2
 */
public class SceneHandlerUtil {

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
     * 直接在当前线程执行，Future 会自动切换到调用线程
     */
    public static void handleRpcResponse(NetSession session, Object message, MethodHandle method, Object bean) {
        try {
            method.invoke(bean, session, message);
        } catch (Throwable e) {
            LoggerUtil.error("RPC 响应处理异常", e);
            throw new RuntimeException(e);
        }
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
