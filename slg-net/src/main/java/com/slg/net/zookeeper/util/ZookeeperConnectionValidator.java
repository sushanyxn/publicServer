package com.slg.net.zookeeper.util;

import com.slg.common.log.LoggerUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;

import java.util.concurrent.TimeUnit;

/**
 * Zookeeper 连接校验器
 * 检查 CuratorFramework 客户端是否已成功连接到 Zookeeper
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class ZookeeperConnectionValidator {

    private final CuratorFramework curatorFramework;

    public ZookeeperConnectionValidator(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    /**
     * 验证 Zookeeper 连接
     * 等待连接建立（最多 10 秒），然后检查连接状态
     *
     * @return true 连接正常，false 连接失败
     */
    public boolean validateConnection() {
        try {
            if (curatorFramework.getState() != CuratorFrameworkState.STARTED) {
                LoggerUtil.error("CuratorFramework 尚未启动");
                return false;
            }

            boolean connected = curatorFramework.blockUntilConnected(10, TimeUnit.SECONDS);
            if (connected) {
                LoggerUtil.debug("Zookeeper 连接验证成功");
                return true;
            } else {
                LoggerUtil.error("Zookeeper 连接超时（10秒内未建立连接）");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LoggerUtil.error("Zookeeper 连接验证被中断", e);
            return false;
        } catch (Exception e) {
            LoggerUtil.error("Zookeeper 连接验证失败", e);
            return false;
        }
    }
}
