package com.slg.net.rpc.facade;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRespone;
import com.slg.net.rpc.manager.RpcCallBackManager;
import com.slg.net.rpc.manager.RpcProxyManager;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.route.redis.RedisRoutePublisher;

/**
 * Redis RPC 消息处理门面
 * 与 {@link RpcFacade} 并列，处理「来自 Redis Stream 的 RPC 请求与响应」
 *
 * <p>区别于 RpcFacade：
 * <ul>
 *   <li>不持有 NetSession，响应通过 {@link RedisRoutePublisher} 写回 Redis resp Stream</li>
 *   <li>isExpired() 由 {@link com.slg.net.rpc.route.redis.RpcRedisRouteConsumerRunner} 在调用前判断，
 *       但此处仍二次校验以防范并发情况</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/03/04
 */
public class RpcRedisFacade {

    private final RpcProxyManager rpcProxyManager;
    private final RpcCallBackManager callBackManager;
    private final RedisRoutePublisher redisRoutePublisher;

    public RpcRedisFacade(RpcProxyManager rpcProxyManager,
                          RpcCallBackManager callBackManager,
                          RedisRoutePublisher redisRoutePublisher) {
        this.rpcProxyManager = rpcProxyManager;
        this.callBackManager = callBackManager;
        this.redisRoutePublisher = redisRoutePublisher;
    }

    /**
     * 处理来自 Redis Stream 的 RPC 请求
     * 逻辑与 RpcFacade.reciveRpcRequest 一致，响应通过 Redis 发回
     *
     * @param request RPC 请求消息（已由 Runner 解码）
     */
    public void reciveRpcRequest(IM_RpcRequest request) {
        RpcMethodMeta meta = rpcProxyManager.getRpcMethodMeta(request.getMethodMarker());
        if (meta == null || meta.getBean() == null || meta.getMethodHandle() == null) {
            LoggerUtil.error("[RpcRedis] 方法未注册或未实现: {}", request.getMethodMarker());
            sendErrorResp(request, "方法未注册或未实现: " + request.getMethodMarker());
            return;
        }

        if (request.isExpired()) {
            LoggerUtil.warn("[RpcRedis] 拒绝处理过期请求: {}, 剩余: {}ms",
                    request.getMethodMarker(), request.getRemainingTimeMillis());
            return;
        }

        try {
            Object result = meta.getMethodHandle().invoke(meta.getBean(), request.getParams());
            if (request.getCallBackId() > 0) {
                handleResult(request, result);
            }
        } catch (Throwable e) {
            LoggerUtil.error("[RpcRedis] 方法执行异常: method={}", request.getMethodMarker(), e);
            sendErrorResp(request, e.getMessage());
        }
    }

    /**
     * 处理来自 Redis Stream 的 RPC 响应
     * 逻辑与 RpcFacade.reciveRpcRespone 一致
     *
     * @param response RPC 响应消息（已由 Runner 解码）
     */
    public void reciveRpcRespone(IM_RpcRespone response) {
        if (response.getError() != null) {
            callBackManager.triggerError(response.getId(), response.getError());
            LoggerUtil.debug("[RpcRedis] 接收到错误响应: callBackId={}, error={}",
                    response.getId(), response.getError());
        } else {
            callBackManager.triggerCallback(response.getId(), response.getResult());
        }
    }

    private void handleResult(IM_RpcRequest request, Object result) {
        if (result instanceof java.util.concurrent.CompletableFuture) {
            java.util.concurrent.CompletableFuture<?> future =
                    (java.util.concurrent.CompletableFuture<?>) result;

            future.whenComplete((actualResult, error) -> {
                if (error != null) {
                    LoggerUtil.error("[RpcRedis] 异步方法执行异常: method={}, callBackId={}",
                            request.getMethodMarker(), request.getCallBackId(), error);
                    sendErrorResp(request,
                            error.getCause() != null ? error.getCause().getMessage() : error.getMessage());
                } else {
                    if (request.isExpired()) {
                        LoggerUtil.warn("[RpcRedis] 异步方法完成时已超过 deadline: method={}, callBackId={}",
                                request.getMethodMarker(), request.getCallBackId());
                    } else {
                        sendSuccessResp(request, actualResult);
                    }
                }
            });
        } else {
            sendSuccessResp(request, result);
        }
    }

    private void sendSuccessResp(IM_RpcRequest request, Object result) {
        IM_RpcRespone response = new IM_RpcRespone();
        response.setId(request.getCallBackId());
        response.setResult(result);
        redisRoutePublisher.publishResp(request.getSourceServerId(), response);
    }

    private void sendErrorResp(IM_RpcRequest request, String error) {
        if (request.getCallBackId() > 0) {
            IM_RpcRespone response = new IM_RpcRespone();
            response.setId(request.getCallBackId());
            response.setError(error);
            redisRoutePublisher.publishResp(request.getSourceServerId(), response);
        }
    }
}
