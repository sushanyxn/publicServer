package com.slg.net.message.innermessage.rpc.packet;

import lombok.Data;

/**
 * RPC 请求消息
 * 支持 Deadline 机制（借鉴 gRPC）
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Data
public class IM_RpcRequest {

    /**
     * 来源服务器ID
     */
    private int sourceServerId;

    /**
     * RPC 方法标识：接口全限定名 + "#" + 方法名
     */
    private String methodMarker;

    /**
     * RPC 方法传参
     */
    private Object[] params;

    /**
     * 请求ID（用于响应匹配，0 表示无需响应）
     */
    private long callBackId;

    /**
     * 截止时间（绝对时间戳，毫秒）
     * 借鉴 gRPC 的 Deadline 机制，使用绝对时间而非相对超时
     * 0 表示无超时限制
     */
    private long deadlineMillis;

    /**
     * 创建 RPC 请求
     */
    public static IM_RpcRequest valueOf(int sourceServerId, String methodMarker, Object[] params, 
                                        long callBackId, long deadlineMillis) {
        IM_RpcRequest request = new IM_RpcRequest();
        request.sourceServerId = sourceServerId;
        request.methodMarker = methodMarker;
        request.params = params;
        request.callBackId = callBackId;
        request.deadlineMillis = deadlineMillis;
        return request;
    }

    /**
     * 获取剩余时间（毫秒）
     */
    public long getRemainingTimeMillis() {
        if (deadlineMillis <= 0) {
            return Long.MAX_VALUE;
        }
        return deadlineMillis - System.currentTimeMillis();
    }

    /**
     * 判断请求是否已过期
     */
    public boolean isExpired() {
        return deadlineMillis > 0 && System.currentTimeMillis() >= deadlineMillis;
    }

}
