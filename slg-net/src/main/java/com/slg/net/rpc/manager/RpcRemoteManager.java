package com.slg.net.rpc.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.rpc.exception.RpcException;
import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.route.IRouteSupportService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 远程调用管理器
 * 负责处理远程 RPC 调用，包括消息构建、发送、回调注册等
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class RpcRemoteManager {

    private final RpcCallBackManager callBackManager;
    private final IRouteSupportService routeSupportService;

    public RpcRemoteManager(
            @Lazy RpcCallBackManager callBackManager,
            @Lazy IRouteSupportService routeSupportService) {
        this.callBackManager = callBackManager;
        this.routeSupportService = routeSupportService;
    }

    /**
     * 回调 ID 生成器
     */
    private final AtomicLong callBackIdGenerator = new AtomicLong(0);

    /**
     * 执行远程调用
     *
     * @param meta   方法元数据
     * @param params 调用参数
     * @return void 方法返回 null，有返回值的方法返回 CompletableFuture
     */
    public Object invokeRemote(RpcMethodMeta meta, Object[] params) {
        // 1. 判断是否有返回值
        boolean hasReturnValue = hasReturnValue(meta.getMethod());

        // 2. 生成唯一的 callBackId（有返回值时才需要）
        long callBackId = hasReturnValue ? generateCallBackId() : 0;

        // 3. 计算 deadline（当前时间 + 方法配置的超时时间）
        long deadlineMillis = System.currentTimeMillis() + meta.getTimeoutMillis();

        // 4. 提取路由参数（提前提取，用于解析目标服务器ID和后续发送）
        Object[] routeParams = extractRouteParams(meta, params);

        // 5. 解析目标服务器ID（用于 RPC 回调的断线清理）
        int targetServerId = meta.getRouteInstance().resolveTargetServerId(meta, routeParams);

        // 6. 创建 Future（如果有返回值）
        CompletableFuture<Object> future = null;
        if (hasReturnValue) {
            future = new CompletableFuture<>();
            callBackManager.registerCallback(callBackId, future, deadlineMillis,
                    meta.getMethodMarker(), targetServerId);
        }

        // 7. 构建请求消息
        IM_RpcRequest request = IM_RpcRequest.valueOf(
                routeSupportService.getLocalServerId(),
                meta.getMethodMarker(),
                params,
                callBackId,
                deadlineMillis
        );

        // 8. 通过路由发送
        try {
            meta.getRouteInstance().sendMsg(request, meta, routeParams);
            
        } catch (Exception e) {
            LoggerUtil.error("[RPC] 发送远程调用失败: method={}", meta.getMethodMarker(), e);
            // 发送失败，清理回调
            if (future != null) {
                callBackManager.removeCallback(callBackId);
                future.completeExceptionally(new RpcException("发送失败", e));
            }
            throw new RpcException("RPC 发送失败: " + meta.getMethodMarker(), e);
        }

        // 9. 返回结果（void 方法返回 null，有返回值返回 Future）
        return future;
    }

    /**
     * 生成全局唯一的 callBackId
     * 高 32 位：服务器 ID，低 32 位：自增序列
     */
    private long generateCallBackId() {
        long serverId = routeSupportService.getLocalServerId();
        long sequence = callBackIdGenerator.incrementAndGet() & 0xFFFFFFFFL;
        return (serverId << 32) | sequence;
    }

    /**
     * 判断方法是否有返回值
     *
     * @param method 方法对象
     * @return true 表示有返回值（非 void）
     */
    private boolean hasReturnValue(Method method) {
        return !method.getReturnType().equals(Void.TYPE);
    }

    /**
     * 提取路由参数
     * 根据方法元数据中的 routeParamsIndex 提取对应的参数
     *
     * @param meta   方法元数据
     * @param params 所有参数
     * @return 路由参数数组
     */
    private Object[] extractRouteParams(RpcMethodMeta meta, Object[] params) {
        int[] routeParamsIndex = meta.getRouteParamsIndex();
        if (routeParamsIndex == null || routeParamsIndex.length == 0) {
            return new Object[0];
        }

        Object[] routeParams = new Object[routeParamsIndex.length];
        for (int i = 0; i < routeParamsIndex.length; i++) {
            routeParams[i] = params[routeParamsIndex[i]];
        }
        return routeParams;
    }

}
