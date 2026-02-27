package com.slg.net.rpc.handler;

import com.slg.net.socket.model.NetSession;

/**
 * 内部连接断线监听器
 * 由业务模块实现，RpcServerMessageHandler 在 onDisconnect 时回调
 *
 * @author yangxunan
 * @date 2026/02/11
 */
public interface InnerSessionDisconnectListener {

    /**
     * 服务器连接断开时回调
     *
     * @param session 断开的会话
     */
    void onServerDisconnect(NetSession session);
}
