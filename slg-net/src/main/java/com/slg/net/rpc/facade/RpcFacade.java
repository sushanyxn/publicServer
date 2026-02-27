package com.slg.net.rpc.facade;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRespone;
import com.slg.net.rpc.manager.RpcCallBackManager;
import com.slg.net.rpc.manager.RpcProxyManager;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RPC 消息处理门面
 * 处理 RPC 请求和响应消息
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class RpcFacade {

    @Autowired
    private RpcProxyManager rpcProxyManager;

    @Autowired
    private RpcCallBackManager callBackManager;

    /**
     * 处理 RPC 请求消息
     * 接收远程 RPC 调用请求，执行本地方法并返回结果
     * 
     * <p>注意：线程切换由消息分发层处理，此处直接在当前线程执行
     */
    @MessageHandler
    public void reciveRpcRequest(NetSession session, IM_RpcRequest request) {
        // 1. 获取方法元数据
        RpcMethodMeta meta = rpcProxyManager.getRpcMethodMeta(request.getMethodMarker());
        if (meta == null || meta.getBean() == null || meta.getMethodHandle() == null) {
            LoggerUtil.error("[RPC] 方法未注册或未实现: {}", request.getMethodMarker());
            sendErrorResponse(session, request.getCallBackId(), "方法未注册或未实现");
            return;
        }

        // 2. Deadline 检测：拒绝处理过期请求
        if (request.isExpired()) {
            LoggerUtil.warn("[RPC] 拒绝处理过期请求: {}, 剩余时间: {}ms",
                    request.getMethodMarker(), request.getRemainingTimeMillis());
            sendErrorResponse(session, request.getCallBackId(), "DEADLINE_EXCEEDED");
            return;
        }

        // 3. 执行方法
        try {
            Object result = meta.getMethodHandle().invoke(meta.getBean(), request.getParams());

            // 4. 如果需要响应（callBackId > 0），处理返回值
            if (request.getCallBackId() > 0) {
                handleResult(session, request, result);
            }

        } catch (Throwable e) {
            LoggerUtil.error("[RPC] 方法执行异常: method={}", request.getMethodMarker(), e);
            sendErrorResponse(session, request.getCallBackId(), e.getMessage());
        }
    }

    /**
     * 处理方法返回值
     * 如果是 CompletableFuture，注册回调等待完成；否则直接发送
     *
     * @param session 网络会话
     * @param request 请求消息
     * @param result  方法返回值
     */
    private void handleResult(NetSession session, IM_RpcRequest request, Object result) {
        if (result instanceof java.util.concurrent.CompletableFuture) {
            // 异步结果：注册回调，当 Future 完成时发送响应
            java.util.concurrent.CompletableFuture<?> future = 
                (java.util.concurrent.CompletableFuture<?>) result;

            future.whenComplete((actualResult, error) -> {
                if (error != null) {
                    // Future 执行异常
                    LoggerUtil.error("[RPC] 异步方法执行异常: method={}, callBackId={}",
                            request.getMethodMarker(), request.getCallBackId(), error);
                    sendErrorResponse(session, request.getCallBackId(), 
                            error.getCause() != null ? error.getCause().getMessage() : error.getMessage());
                } else {
                    // 检查是否超过 deadline
                    if (request.isExpired()) {
                        LoggerUtil.warn("[RPC] 异步方法完成时已超过 deadline: method={}, callBackId={}",
                                request.getMethodMarker(), request.getCallBackId());
                        sendErrorResponse(session, request.getCallBackId(), "DEADLINE_EXCEEDED");
                    } else {
                        // 发送实际结果
                        sendSuccessResponse(session, request.getCallBackId(), actualResult);
                    }
                }
            });


        } else {
            // 同步结果：直接发送
            sendSuccessResponse(session, request.getCallBackId(), result);
        }
    }

    /**
     * 处理 RPC 响应消息
     * 接收远程方法的返回结果，触发回调
     */
    @MessageHandler
    public void reciveRpcRespone(NetSession session, IM_RpcRespone response) {
        if (response.getError() != null) {
            // 有错误，触发错误回调
            callBackManager.triggerError(response.getId(), response.getError());
            LoggerUtil.debug("[RPC] 接收到错误响应: callBackId={}, error={}",
                    response.getId(), response.getError());
        } else {
            // 正常响应，触发成功回调
            callBackManager.triggerCallback(response.getId(), response.getResult());
        }
    }

    /**
     * 发送成功响应
     */
    private void sendSuccessResponse(NetSession session, long callBackId, Object result) {
        IM_RpcRespone response = new IM_RpcRespone();
        response.setId(callBackId);
        response.setResult(result);
        session.sendMessage(response);
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(NetSession session, long callBackId, String error) {
        if (callBackId > 0) {
            IM_RpcRespone response = new IM_RpcRespone();
            response.setId(callBackId);
            response.setError(error);
            session.sendMessage(response);
        }
    }

}
