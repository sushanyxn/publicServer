package com.slg.net.rpc.manager;

import com.slg.common.executor.core.GlobalScheduler;
import com.slg.common.executor.TaskModule;
import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.exception.RpcException;
import com.slg.net.rpc.exception.RpcTimeoutException;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * RPC 回调管理器
 * 负责管理 RPC 调用的异步回调，支持基于 Deadline 的超时机制
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class RpcCallBackManager {

    /**
     * 回调上下文
     * 封装回调相关的所有信息
     */
    @Getter
    @AllArgsConstructor
    private static class CallbackContext {
        /**
         * 异步结果 Future
         */
        private final CompletableFuture<Object> future;

        /**
         * 超时任务句柄
         */
        private final ScheduledFuture<?> timeoutTask;

        /**
         * 回调创建时间
         */
        private final long createTime;

        /**
         * 方法标识（用于日志）
         */
        private final String methodMarker;

        /**
         * RPC 调用的目标服务器ID
         * 用于连接断开时按 serverId 批量 fail pending 回调
         * 0 表示未知目标服务器
         */
        private final int targetServerId;
    }

    /**
     * 回调映射：callBackId -> CallbackContext
     */
    private final ConcurrentHashMap<Long, CallbackContext> callbacks = new ConcurrentHashMap<>();

    /**
     * 注册回调（基于 Deadline）
     *
     * @param callBackId      回调ID
     * @param future          异步结果
     * @param deadlineMillis  截止时间（绝对时间戳，毫秒）
     * @param methodMarker    方法标识
     * @param targetServerId  目标服务器ID，用于断线时按 serverId 批量 fail；0 表示未知
     */
    public void registerCallback(long callBackId, CompletableFuture<Object> future,
                                  long deadlineMillis, String methodMarker,
                                  int targetServerId) {
        long timeout = deadlineMillis - System.currentTimeMillis();

        if (timeout <= 0) {
            future.completeExceptionally(new RpcTimeoutException("请求已超时"));
            LoggerUtil.error("[RPC] 注册回调失败，已超时: callBackId={}, method={}", callBackId, methodMarker);
            return;
        }

        // 通过 GlobalScheduler 调度超时任务，到点后自动分发到 RPC_RESPONSE 单链执行
        ScheduledFuture<?> timeoutTask = GlobalScheduler.getInstance().schedule(
                TaskModule.RPC_RESPONSE, () -> handleTimeout(callBackId), timeout, TimeUnit.MILLISECONDS);

        CallbackContext context = new CallbackContext(future, timeoutTask,
                System.currentTimeMillis(), methodMarker, targetServerId);
        callbacks.put(callBackId, context);
    }

    /**
     * 触发成功回调
     * 直接在当前线程 complete Future，由 join() 机制自动唤醒等待的虚拟线程
     *
     * @param callBackId 回调ID
     * @param result     返回结果
     */
    public void triggerCallback(long callBackId, Object result) {
        CallbackContext context = callbacks.remove(callBackId);
        if (context == null) {
            LoggerUtil.warn("[RPC] 回调不存在或已超时: callBackId={}", callBackId);
            return;
        }

        cancelTimeoutTask(context);
        context.getFuture().complete(result);
    }

    /**
     * 触发错误回调
     * 直接在当前线程 completeExceptionally，由 join() 机制自动唤醒等待的虚拟线程
     *
     * @param callBackId 回调ID
     * @param error      错误信息
     */
    public void triggerError(long callBackId, String error) {
        CallbackContext context = callbacks.remove(callBackId);
        if (context == null) {
            LoggerUtil.warn("[RPC] 回调不存在或已超时: callBackId={}", callBackId);
            return;
        }

        cancelTimeoutTask(context);

        LoggerUtil.error("[RPC] 回调异常: callBackId={}, method={}, error={}",
                callBackId, context.getMethodMarker(), error);

        // 直接 completeExceptionally，不做线程回投
        context.getFuture().completeExceptionally(new RpcException(error));
    }

    /**
     * 处理超时
     * 直接在超时调度线程 completeExceptionally，由 join() 机制自动唤醒等待的虚拟线程
     *
     * @param callBackId 回调ID
     */
    private void handleTimeout(long callBackId) {
        CallbackContext context = callbacks.remove(callBackId);
        if (context == null) {
            // 已被正常响应处理
            return;
        }

        long cost = System.currentTimeMillis() - context.getCreateTime();
        LoggerUtil.error("[RPC] 调用超时: callBackId={}, method={}, cost={}ms",
                callBackId, context.getMethodMarker(), cost);

        // 直接 completeExceptionally，不做线程回投
        context.getFuture().completeExceptionally(
                new RpcTimeoutException("RPC 调用超时: " + context.getMethodMarker())
        );
    }

    /**
     * 移除回调（用于发送失败等场景）
     *
     * @param callBackId 回调ID
     */
    public void removeCallback(long callBackId) {
        CallbackContext context = callbacks.remove(callBackId);
        if (context != null) {
            cancelTimeoutTask(context);
            LoggerUtil.debug("[RPC] 移除回调: callBackId={}", callBackId);
        }
    }

    /**
     * 连接断开时，主动 fail 该 serverId 上所有 pending 的 RPC 回调
     * 避免调用方等待 30 秒超时
     *
     * @param serverId 断开连接的目标服务器ID
     * @param cause    异常原因
     */
    public void failAllByServerId(int serverId, Throwable cause) {
        java.util.Iterator<java.util.Map.Entry<Long, CallbackContext>> it = callbacks.entrySet().iterator();
        int failedCount = 0;
        while (it.hasNext()) {
            java.util.Map.Entry<Long, CallbackContext> entry = it.next();
            CallbackContext callback = entry.getValue();
            if (callback.getTargetServerId() == serverId) {
                cancelTimeoutTask(callback);
                callback.getFuture().completeExceptionally(cause);
                it.remove();
                failedCount++;
            }
        }
        if (failedCount > 0) {
            LoggerUtil.debug("[RPC] 断线清理: serverId={}, 清理{}个pending回调", serverId, failedCount);
        }
    }

    /**
     * 获取待处理回调数量（用于监控）
     */
    public int getPendingCallbackCount() {
        return callbacks.size();
    }

    /**
     * 取消超时任务（空安全，GlobalScheduler 关闭时 schedule 可能返回 null）
     */
    private void cancelTimeoutTask(CallbackContext context) {
        ScheduledFuture<?> task = context.getTimeoutTask();
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * 关闭管理器
     */
    @PreDestroy
    public void shutdown() {
        LoggerUtil.debug("[RPC] 关闭回调管理器，待处理回调数: {}", callbacks.size());
        callbacks.clear();
    }

}
