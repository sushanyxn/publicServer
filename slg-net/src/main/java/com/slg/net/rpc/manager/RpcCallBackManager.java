package com.slg.net.rpc.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.exception.RpcException;
import com.slg.net.rpc.exception.RpcTimeoutException;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * RPC 回调管理器
 * 负责管理 RPC 调用的异步回调，支持基于 Deadline 的超时机制
 *
 * <p>使用 Netty {@link HashedWheelTimer} 做超时检测，相比 ScheduledExecutorService：
 * <ul>
 *   <li>O(1) 添加/取消定时任务，适合大量短生命周期的 RPC 超时</li>
 *   <li>内存开销更低，无需为每个回调分配独立的 ScheduledFuture 对象</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class RpcCallBackManager {

    /**
     * 回调上下文
     */
    @Getter
    @AllArgsConstructor
    private static class CallbackContext {
        private final CompletableFuture<Object> future;
        private final Timeout timeoutHandle;
        private final long createTime;
        private final String methodMarker;
        /**
         * RPC 调用的目标服务器ID
         * 用于连接断开时按 serverId 批量 fail pending 回调
         * 0 表示未知目标服务器
         */
        private final int targetServerId;
    }

    private final ConcurrentHashMap<Long, CallbackContext> callbacks = new ConcurrentHashMap<>();

    /**
     * tick 100ms，512 格，覆盖 ~51 秒超时范围，满足默认 30 秒 RPC 超时
     */
    private final HashedWheelTimer timeoutTimer = new HashedWheelTimer(
            r -> {
                Thread t = new Thread(r, "rpc-timeout-timer");
                t.setDaemon(true);
                return t;
            },
            100, TimeUnit.MILLISECONDS, 512);

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

        Timeout handle = timeoutTimer.newTimeout(t -> handleTimeout(callBackId), timeout, TimeUnit.MILLISECONDS);

        CallbackContext context = new CallbackContext(future, handle,
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

        context.getFuture().completeExceptionally(new RpcException(error));
    }

    /**
     * 处理超时（由 HashedWheelTimer 工作线程触发）
     * completeExceptionally 后由 join() 机制自动唤醒等待的虚拟线程
     */
    private void handleTimeout(long callBackId) {
        CallbackContext context = callbacks.remove(callBackId);
        if (context == null) {
            return;
        }

        long cost = System.currentTimeMillis() - context.getCreateTime();
        LoggerUtil.error("[RPC] 调用超时: callBackId={}, method={}, cost={}ms",
                callBackId, context.getMethodMarker(), cost);

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
        Iterator<Map.Entry<Long, CallbackContext>> it = callbacks.entrySet().iterator();
        int failedCount = 0;
        while (it.hasNext()) {
            Map.Entry<Long, CallbackContext> entry = it.next();
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

    private void cancelTimeoutTask(CallbackContext context) {
        Timeout handle = context.getTimeoutHandle();
        if (handle != null) {
            handle.cancel();
        }
    }

    @PreDestroy
    public void shutdown() {
        LoggerUtil.debug("[RPC] 关闭回调管理器，待处理回调数: {}", callbacks.size());
        timeoutTimer.stop();
        callbacks.clear();
    }

}
