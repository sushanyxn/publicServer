package com.slg.net.zookeeper.writer;

import com.slg.net.zookeeper.constant.ZkPath;
import com.slg.net.zookeeper.service.ZookeeperShareService;

/**
 * SceneServer ZK 节点写入器
 * 持有本服 serverId，提供 Scene 相关字段的类型安全写入方法
 *
 * <p>所有写入操作委托给 {@link ZookeeperShareService}，
 * 写入后 watcher 会自动同步到 ZKConfig
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class SceneServerZkWriter {

    private final int serverId;
    private final ZookeeperShareService shareService;

    public SceneServerZkWriter(int serverId, ZookeeperShareService shareService) {
        this.serverId = serverId;
        this.shareService = shareService;
    }

    /**
     * 写入绑定的 GameServer ID
     *
     * @param gameId GameServer ID
     */
    public void writeBindGameId(int gameId) {
        shareService.writeSceneServerField(serverId, ZkPath.BIND_GAME_ID, String.valueOf(gameId));
    }

    /**
     * 写入是否启用
     *
     * @param enable 是否启用
     */
    public void writeEnable(boolean enable) {
        shareService.writeSceneServerField(serverId, ZkPath.ENABLE, String.valueOf(enable));
    }

    /**
     * 写入数据库版本号
     *
     * @param dbVersion 版本号
     */
    public void writeDbVersion(String dbVersion) {
        shareService.writeSceneServerField(serverId, ZkPath.DB_VERSION, dbVersion);
    }

    /**
     * 获取本服 serverId
     *
     * @return serverId
     */
    public int getServerId() {
        return serverId;
    }
}
