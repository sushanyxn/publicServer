package com.slg.net.rpc.exception;

/**
 * RPC 超时异常
 * 当 RPC 调用超过 Deadline 时抛出
 *
 * @author yangxunan
 * @date 2026/01/23
 */
public class RpcTimeoutException extends RpcException {

    public RpcTimeoutException(String message) {
        super(message);
    }

}

