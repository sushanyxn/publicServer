package com.slg.net.zookeeper.writer;

import com.slg.net.zookeeper.constant.ZkPath;
import com.slg.net.zookeeper.service.ZookeeperShareService;

/**
 * GameServer ZK 节点写入器
 * 持有本服 serverId，提供 Game 相关字段的类型安全写入方法
 *
 * <p>所有写入操作委托给 {@link ZookeeperShareService}，
 * 写入后 watcher 会自动同步到 ZKConfig
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class GameServerZkWriter {

    private final int serverId;
    private final ZookeeperShareService shareService;

    public GameServerZkWriter(int serverId, ZookeeperShareService shareService) {
        this.serverId = serverId;
        this.shareService = shareService;
    }

    /**
     * 写入合服版本号
     *
     * @param version 合服版本号
     */
    public void writeMergeServerVersion(int version) {
        shareService.writeGameServerField(serverId, ZkPath.MERGE_SERVER_VERSION, String.valueOf(version));
    }

    /**
     * 写入已注册角色数
     *
     * @param count 角色数
     */
    public void writeRegistedRole(long count) {
        shareService.writeGameServerField(serverId, ZkPath.REGISTED_ROLE, String.valueOf(count));
    }

    /**
     * 写入导量开关
     *
     * @param switchValue 开关值：close/open/auto
     */
    public void writeDiversionSwitch(String switchValue) {
        shareService.writeGameServerField(serverId, ZkPath.DIVERSION_SWITCH, switchValue);
    }

    /**
     * 写入导量配置
     *
     * @param configJson 导量配置 JSON 字符串
     */
    public void writeDiversionConfig(String configJson) {
        shareService.writeGameServerField(serverId, ZkPath.DIVERSION_CONFIG, configJson);
    }

    /**
     * 写入是否多角色服显示
     *
     * @param show 是否显示
     */
    public void writeMultiRoleServerShow(boolean show) {
        shareService.writeGameServerField(serverId, ZkPath.MULTI_ROLE_SERVER_SHOW, String.valueOf(show));
    }

    /**
     * 写入是否出现在服务器列表
     *
     * @param inList 是否出现
     */
    public void writeInServerList(boolean inList) {
        shareService.writeGameServerField(serverId, ZkPath.IN_SERVER_LIST, String.valueOf(inList));
    }

    /**
     * 写入是否启用
     *
     * @param enable 是否启用
     */
    public void writeEnable(boolean enable) {
        shareService.writeGameServerField(serverId, ZkPath.ENABLE, String.valueOf(enable));
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
