package com.slg.scene.net.handler;

import com.slg.common.executor.Executor;
import com.slg.net.rpc.handler.InnerSessionDisconnectListener;
import com.slg.net.socket.model.NetSession;
import com.slg.scene.net.manager.InnerSessionManager;
import org.springframework.stereotype.Component;

/**
 * Scene 侧内部连接断线监听器
 * 实现 InnerSessionDisconnectListener，将 removeSession 从 IO 线程分发到 System 链执行
 * 消除 IO 线程与 System 链的竞态
 *
 * @author yangxunan
 * @date 2026/02/11
 */
@Component
public class SceneDisconnectListener implements InnerSessionDisconnectListener {

    @Override
    public void onServerDisconnect(NetSession session) {
        // 从 IO 线程分发到 System 链，与 cancelPendingCleanup 串行
        Executor.System.execute(() -> {
            InnerSessionManager.getInstance().removeSession(session);
        });
    }
}
