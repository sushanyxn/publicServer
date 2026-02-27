package com.slg.net.zookeeper.model;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zookeeper 配置数据中心
 * 启动时一次性加载所有 GameServer 和 SceneServer 信息，并通过监听器自动同步变更
 *
 * <p>线程安全：内部使用 ConcurrentHashMap 存储，支持并发读写
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class ZKConfig {

    private final Map<Integer, GameServerZkInfo> gameServers = new ConcurrentHashMap<>();
    private final Map<Integer, SceneServerZkInfo> sceneServers = new ConcurrentHashMap<>();

    /**
     * 获取指定 GameServer 配置
     *
     * @param serverId 服务器ID
     * @return GameServerZkInfo，不存在返回 null
     */
    public GameServerZkInfo getGameServer(int serverId) {
        return gameServers.get(serverId);
    }

    /**
     * 获取指定 SceneServer 配置
     *
     * @param serverId 服务器ID
     * @return SceneServerZkInfo，不存在返回 null
     */
    public SceneServerZkInfo getSceneServer(int serverId) {
        return sceneServers.get(serverId);
    }

    /**
     * 获取所有 GameServer 配置（只读视图）
     *
     * @return 不可修改的 serverId -> GameServerZkInfo 映射
     */
    public Map<Integer, GameServerZkInfo> getAllGameServers() {
        return Collections.unmodifiableMap(gameServers);
    }

    /**
     * 获取所有 SceneServer 配置（只读视图）
     *
     * @return 不可修改的 serverId -> SceneServerZkInfo 映射
     */
    public Map<Integer, SceneServerZkInfo> getAllSceneServers() {
        return Collections.unmodifiableMap(sceneServers);
    }

    // ========================= 更新方法（由 ZookeeperShareService 调用，业务代码不应直接调用） =========================

    public void putGameServer(int serverId, GameServerZkInfo info) {
        gameServers.put(serverId, info);
    }

    public void putSceneServer(int serverId, SceneServerZkInfo info) {
        sceneServers.put(serverId, info);
    }

    public void removeGameServer(int serverId) {
        gameServers.remove(serverId);
    }

    public void removeSceneServer(int serverId) {
        sceneServers.remove(serverId);
    }
}
