package com.slg.net.rpc.exception;

/**
 * RPC 基础异常
 * 所有 RPC 相关异常的基类
 *
 * @author yangxunan
 * @date 2026/01/23
 */
public class RpcException extends RuntimeException {

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

}

