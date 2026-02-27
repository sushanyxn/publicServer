package com.slg.net.rpc.exception;

/**
 * RPC 断线异常
 * 当连接断开导致 RPC 调用失败时抛出，调用方可据此区分断线失败和业务异常
 *
 * @author yangxunan
 * @date 2026/02/11
 */
public class RpcDisconnectException extends RpcException {

    public RpcDisconnectException(String message) {
        super(message);
    }

    public RpcDisconnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
