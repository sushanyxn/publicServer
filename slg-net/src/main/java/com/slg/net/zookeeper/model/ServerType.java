package com.slg.net.zookeeper.model;

import com.slg.net.zookeeper.constant.ZkPath;
import lombok.Getter;

/**
 * 服务器类型枚举
 * 区分 GameServer 和 SceneServer 在 ZK 中的路径和标记
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Getter
public enum ServerType {

    /** 游戏服务器 */
    GAME(ZkPath.GAME_SERVERS, ZkPath.GAME_CONFIG_END_FLAG),

    /** 场景服务器 */
    SCENE(ZkPath.SCENE_SERVERS, ZkPath.SCENE_CONFIG_END_FLAG);

    /** ZK 一级路径（如 /GameServers） */
    private final String basePath;

    /** 配置完成标记节点名 */
    private final String configEndFlag;

    ServerType(String basePath, String configEndFlag) {
        this.basePath = basePath;
        this.configEndFlag = configEndFlag;
    }

    /**
     * 获取指定 serverId 的完整路径
     *
     * @param serverId 服务器ID
     * @return 完整路径，如 /GameServers/1
     */
    public String serverPath(int serverId) {
        return basePath + "/" + serverId;
    }
}
