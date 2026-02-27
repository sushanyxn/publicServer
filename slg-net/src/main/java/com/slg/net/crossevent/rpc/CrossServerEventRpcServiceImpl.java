package com.slg.net.crossevent.rpc;

import com.slg.common.event.manager.EventBusManager;
import com.slg.common.event.model.IEvent;
import com.slg.common.log.LoggerUtil;
import com.slg.net.crossevent.bridge.CrossServerEventBridge;
import org.springframework.stereotype.Component;

/**
 * 跨服事件分发 RPC 实现
 * 接收远程事件后在本地发布，通过 ThreadLocal 防重入标记避免循环转发
 *
 * @author yangxunan
 * @date 2026/02/13
 */
@Component
public class CrossServerEventRpcServiceImpl implements ICrossServerEventRpcService {

    @Override
    public void dispatchByServer(int serverId, Object eventVO) {
        publishLocally(eventVO);
    }

    @Override
    public void dispatchByPlayerGame(long playerId, Object eventVO) {
        publishLocally(eventVO);
    }

    @Override
    public void dispatchByPlayerMainScene(long playerId, Object eventVO) {
        publishLocally(eventVO);
    }

    @Override
    public void dispatchByPlayerCurrentScene(long playerId, Object eventVO) {
        publishLocally(eventVO);
    }

    /**
     * 在本地发布事件，设置防重入标记防止循环转发
     *
     * @param eventVO 事件 VO 对象
     */
    private void publishLocally(Object eventVO) {
        if (eventVO instanceof IEvent event) {
            // 设置防重入标记，Bridge 监听器检测到后跳过转发
            CrossServerEventBridge.setReceiving(true);
            try {
                EventBusManager.getInstance().publishEvent(event);
            } finally {
                CrossServerEventBridge.setReceiving(false);
            }
        } else {
            LoggerUtil.warn("[跨服事件] 接收到非 IEvent 类型的事件: {}",
                    eventVO != null ? eventVO.getClass().getSimpleName() : "null");
        }
    }
}
