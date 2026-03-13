package com.slg.net.zookeeper.service;

import com.slg.common.log.LoggerUtil;
import com.slg.net.zookeeper.constant.ZkPath;
import com.slg.net.zookeeper.model.*;
import lombok.Getter;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Zookeeper 服务器注册与信息共享服务
 * 基于逐字段子节点方式读写 GameServer 和 SceneServer 的注册信息
 *
 * <p>节点存储规则：
 * <ul>
 *   <li>每个配置项是独立的子节点，值存储为节点 data</li>
 *   <li>Redis/ 和 MongoDB/ 为二级子树</li>
 *   <li>instance 为 EPHEMERAL 临时节点，用于进程存活检测</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Getter
public class ZookeeperShareService {

    private final CuratorFramework curatorFramework;
    private final ZookeeperConfigService configService;
    private final ZKConfig zkConfig;

    public ZookeeperShareService(CuratorFramework curatorFramework,
                                 ZookeeperConfigService configService,
                                 ZKConfig zkConfig) {
        this.curatorFramework = curatorFramework;
        this.configService = configService;
        this.zkConfig = zkConfig;
    }

    // ========================= 数据加载与监听 =========================

    /**
     * 一次性加载 ZK 中所有 GameServer 和 SceneServer 数据到 ZKConfig
     * 在 ZK 连接成功后由生命周期调用
     */
    public void loadAll() {
        List<GameServerZkInfo> games = readAllGameServers();
        for (GameServerZkInfo info : games) {
            zkConfig.putGameServer(info.getServerId(), info);
        }
        LoggerUtil.debug("ZKConfig 加载完成: GameServer {} 个", games.size());

        List<SceneServerZkInfo> scenes = readAllSceneServers();
        for (SceneServerZkInfo info : scenes) {
            zkConfig.putSceneServer(info.getServerId(), info);
        }
        LoggerUtil.debug("ZKConfig 加载完成: SceneServer {} 个", scenes.size());
    }

    /**
     * 监听 /GameServers 和 /SceneServers 整棵子树的变化
     * 采用增量更新策略：普通字段直接用回调数据更新，Redis/Mongo 子树回退为重读子树
     */
    public void watchAll() {
        configService.watchConfig(ZkPath.GAME_SERVERS, (path, data) -> {
            Integer serverId = parseServerIdFromPath(path, ZkPath.GAME_SERVERS);
            if (serverId == null) {
                return;
            }
            String fieldName = parseFieldFromPath(path, ZkPath.GAME_SERVERS);
            onGameServerNodeChanged(serverId, fieldName, data);
        }, true);

        configService.watchConfig(ZkPath.SCENE_SERVERS, (path, data) -> {
            Integer serverId = parseServerIdFromPath(path, ZkPath.SCENE_SERVERS);
            if (serverId == null) {
                return;
            }
            String fieldName = parseFieldFromPath(path, ZkPath.SCENE_SERVERS);
            onSceneServerNodeChanged(serverId, fieldName, data);
        }, true);

        LoggerUtil.debug("ZKConfig 监听已启动");
    }

    /**
     * 停止所有监听，释放 CuratorCache 资源
     */
    public void unwatchAll() {
        configService.unwatchAll();
        LoggerUtil.debug("ZKConfig 监听已停止");
    }

    /**
     * GameServer 节点变化的增量处理
     *
     * @param serverId  服务器ID
     * @param fieldName 变化的字段名（serverId 下的第一级子节点名），null 表示 serverId 根节点
     * @param data      新值，null 表示删除事件
     */
    private void onGameServerNodeChanged(int serverId, String fieldName, String data) {
        if (fieldName == null) {
            if (data == null) {
                zkConfig.removeGameServer(serverId);
                LoggerUtil.debug("ZKConfig GameServer serverId={} 已移除", serverId);
            } else {
                reloadGameServer(serverId);
            }
            return;
        }

        GameServerZkInfo info = zkConfig.getGameServer(serverId);
        if (info == null) {
            reloadGameServer(serverId);
            return;
        }

        if (ZkPath.REDIS.equals(fieldName)) {
            info.setRedis(readRedisInfo(ZkPath.gameServerPath(serverId)));
            LoggerUtil.debug("ZKConfig GameServer serverId={} Redis 子树已更新", serverId);
        } else if (ZkPath.MONGO.equals(fieldName)) {
            info.setMongo(readMongoInfo(ZkPath.gameServerPath(serverId)));
            LoggerUtil.debug("ZKConfig GameServer serverId={} MongoDB 子树已更新", serverId);
        } else if (info.updateField(fieldName, data)) {
            LoggerUtil.debug("ZKConfig GameServer serverId={} 字段 {} 已增量更新", serverId, fieldName);
        }
    }

    /**
     * SceneServer 节点变化的增量处理
     *
     * @param serverId  服务器ID
     * @param fieldName 变化的字段名（serverId 下的第一级子节点名），null 表示 serverId 根节点
     * @param data      新值，null 表示删除事件
     */
    private void onSceneServerNodeChanged(int serverId, String fieldName, String data) {
        if (fieldName == null) {
            if (data == null) {
                zkConfig.removeSceneServer(serverId);
                LoggerUtil.debug("ZKConfig SceneServer serverId={} 已移除", serverId);
            } else {
                reloadSceneServer(serverId);
            }
            return;
        }

        SceneServerZkInfo info = zkConfig.getSceneServer(serverId);
        if (info == null) {
            reloadSceneServer(serverId);
            return;
        }

        if (ZkPath.REDIS.equals(fieldName)) {
            info.setRedis(readRedisInfo(ZkPath.sceneServerPath(serverId)));
            LoggerUtil.debug("ZKConfig SceneServer serverId={} Redis 子树已更新", serverId);
        } else if (ZkPath.MONGO.equals(fieldName)) {
            info.setMongo(readMongoInfo(ZkPath.sceneServerPath(serverId)));
            LoggerUtil.debug("ZKConfig SceneServer serverId={} MongoDB 子树已更新", serverId);
        } else if (info.updateField(fieldName, data)) {
            LoggerUtil.debug("ZKConfig SceneServer serverId={} 字段 {} 已增量更新", serverId, fieldName);
        }
    }

    /**
     * 全量重新加载指定 GameServer 数据到 ZKConfig
     * 仅在本地无缓存或 serverId 根节点新增时使用
     *
     * @param serverId 服务器ID
     */
    private void reloadGameServer(int serverId) {
        String base = ZkPath.gameServerPath(serverId);
        if (!configService.exists(base)) {
            zkConfig.removeGameServer(serverId);
            LoggerUtil.debug("ZKConfig GameServer serverId={} 已移除（节点不存在）", serverId);
            return;
        }
        GameServerZkInfo info = readGameServer(serverId);
        if (info != null) {
            zkConfig.putGameServer(serverId, info);
            LoggerUtil.debug("ZKConfig GameServer serverId={} 已全量加载", serverId);
        }
    }

    /**
     * 全量重新加载指定 SceneServer 数据到 ZKConfig
     * 仅在本地无缓存或 serverId 根节点新增时使用
     *
     * @param serverId 服务器ID
     */
    private void reloadSceneServer(int serverId) {
        String base = ZkPath.sceneServerPath(serverId);
        if (!configService.exists(base)) {
            zkConfig.removeSceneServer(serverId);
            LoggerUtil.debug("ZKConfig SceneServer serverId={} 已移除（节点不存在）", serverId);
            return;
        }
        SceneServerZkInfo info = readSceneServer(serverId);
        if (info != null) {
            zkConfig.putSceneServer(serverId, info);
            LoggerUtil.debug("ZKConfig SceneServer serverId={} 已全量加载", serverId);
        }
    }

    /**
     * 从变化路径中解析出 serverId
     * 路径格式如：/GameServers/1/mergeServerVersion 或 /GameServers/1
     *
     * @param path       变化的完整路径
     * @param serverRoot 服务器根路径（如 /GameServers）
     * @return serverId，解析失败返回 null
     */
    private Integer parseServerIdFromPath(String path, String serverRoot) {
        if (path == null || !path.startsWith(serverRoot + "/")) {
            return null;
        }
        String remainder = path.substring(serverRoot.length() + 1);
        int slashIdx = remainder.indexOf('/');
        String idStr = slashIdx > 0 ? remainder.substring(0, slashIdx) : remainder;
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从变化路径中解析出字段名（serverId 之后的第一级子节点名）
     * <p>示例：
     * <ul>
     *   <li>/GameServers/1/enable -> "enable"</li>
     *   <li>/GameServers/1/Redis/host -> "Redis"</li>
     *   <li>/GameServers/1 -> null（serverId 根节点）</li>
     * </ul>
     *
     * @param path       变化的完整路径
     * @param serverRoot 服务器根路径（如 /GameServers）
     * @return 字段名，serverId 根节点变化时返回 null
     */
    private String parseFieldFromPath(String path, String serverRoot) {
        String remainder = path.substring(serverRoot.length() + 1);
        int firstSlash = remainder.indexOf('/');
        if (firstSlash < 0) {
            return null;
        }
        String afterServerId = remainder.substring(firstSlash + 1);
        int secondSlash = afterServerId.indexOf('/');
        return secondSlash > 0 ? afterServerId.substring(0, secondSlash) : afterServerId;
    }

    // ========================= 单字段写入（供 Writer 调用） =========================

    /**
     * 写入 GameServer 的单个字段到 ZK
     * 写入后 watcher 会自动触发增量更新同步到 ZKConfig
     *
     * @param serverId 服务器ID
     * @param field    字段名（ZkPath 中的常量）
     * @param value    字段值
     */
    public void writeGameServerField(int serverId, String field, String value) {
        String base = ZkPath.gameServerPath(serverId);
        writeField(base, field, value);
    }

    /**
     * 写入 SceneServer 的单个字段到 ZK
     * 写入后 watcher 会自动触发增量更新同步到 ZKConfig
     *
     * @param serverId 服务器ID
     * @param field    字段名（ZkPath 中的常量）
     * @param value    字段值
     */
    public void writeSceneServerField(int serverId, String field, String value) {
        String base = ZkPath.sceneServerPath(serverId);
        writeField(base, field, value);
    }

    // ========================= Instance 临时节点管理 =========================

    /**
     * 创建 instance 临时节点（进程启动时调用）
     * 连接断开时 ZK 自动销毁该节点
     *
     * @param serverId 服务器ID
     * @param type     服务器类型
     * @return true 创建成功
     */
    public boolean createInstance(int serverId, ServerType type) {
        String basePath = type.serverPath(serverId);
        String path = ZkPath.instancePath(basePath);
        try {
            Stat stat = curatorFramework.checkExists().forPath(path);
            if (stat != null) {
                curatorFramework.delete().forPath(path);
            }
            String timestamp = String.valueOf(System.currentTimeMillis());
            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(path, timestamp.getBytes(StandardCharsets.UTF_8));
            LoggerUtil.debug("{} serverId={} instance 节点创建成功", type.name(), serverId);
            return true;
        } catch (Exception e) {
            LoggerUtil.error("{} serverId={} instance 节点创建失败", type.name(), serverId, e);
            return false;
        }
    }

    /**
     * 主动销毁 instance 临时节点（优雅关闭时调用）
     *
     * @param serverId 服务器ID
     * @param type     服务器类型
     */
    public void destroyInstance(int serverId, ServerType type) {
        String basePath = type.serverPath(serverId);
        String path = ZkPath.instancePath(basePath);
        try {
            Stat stat = curatorFramework.checkExists().forPath(path);
            if (stat != null) {
                curatorFramework.delete().forPath(path);
                LoggerUtil.debug("{} serverId={} instance 节点已销毁", type.name(), serverId);
            }
        } catch (Exception e) {
            LoggerUtil.error("{} serverId={} instance 节点销毁失败", type.name(), serverId, e);
        }
    }

    /**
     * 检查服务器进程是否存活
     *
     * @param serverId 服务器ID
     * @param type     服务器类型
     * @return true 进程存活（instance 节点存在）
     */
    public boolean isAlive(int serverId, ServerType type) {
        String basePath = type.serverPath(serverId);
        String path = ZkPath.instancePath(basePath);
        return configService.exists(path);
    }

    // ========================= GameServer 读写 =========================

    /**
     * 将 GameServerZkInfo 逐字段写入 ZK 子节点
     *
     * @param info GameServer 注册信息
     */
    public void writeGameServer(GameServerZkInfo info) {
        String base = ZkPath.gameServerPath(info.getServerId());
        writeField(base, ZkPath.GAME_IP, info.getGameIp());
        writeField(base, ZkPath.GAME_HOST, info.getGameHost());
        writeField(base, ZkPath.GAME_PORT, String.valueOf(info.getGamePort()));
        writeField(base, ZkPath.RPC_IP, info.getRpcIp());
        writeField(base, ZkPath.RPC_PORT, String.valueOf(info.getRpcPort()));
        writeField(base, ZkPath.ENABLE, String.valueOf(info.isEnable()));
        writeField(base, ZkPath.IN_SERVER_LIST, String.valueOf(info.isInServerList()));
        writeField(base, ZkPath.OPEN_TIME_MS, String.valueOf(info.getOpenTimeMs()));
        writeField(base, ZkPath.REGISTED_ROLE, String.valueOf(info.getRegistedRole()));
        writeField(base, ZkPath.DB_VERSION, info.getDbVersion());
        writeField(base, ZkPath.TIME_ZONE_OFFSET, String.valueOf(info.getTimeZoneOffset()));
        writeField(base, ZkPath.MERGE_SERVER_VERSION, String.valueOf(info.getMergeServerVersion()));
        writeField(base, ZkPath.MULTI_ROLE_SERVER_SHOW, String.valueOf(info.isMultiRoleServerShow()));

        writeRedisInfo(base, info.getRedis());
        writeMongoInfo(base, info.getMongo());

        writeField(base, ZkPath.GAME_CONFIG_END_FLAG, ZkPath.GAME_CONFIG_END_FLAG);
        LoggerUtil.debug("GameServer serverId={} 信息写入 ZK 完成", info.getServerId());
    }

    /**
     * 从 ZK 子节点读取并组装为 GameServerZkInfo
     *
     * @param serverId 服务器ID
     * @return GameServerZkInfo，节点不存在返回 null
     */
    public GameServerZkInfo readGameServer(int serverId) {
        String base = ZkPath.gameServerPath(serverId);
        if (!configService.exists(base)) {
            return null;
        }

        GameServerZkInfo info = new GameServerZkInfo();
        info.setServerId(serverId);
        info.setGameIp(readField(base, ZkPath.GAME_IP));
        info.setGameHost(readField(base, ZkPath.GAME_HOST));
        info.setGamePort(readIntField(base, ZkPath.GAME_PORT, 0));
        info.setRpcIp(readField(base, ZkPath.RPC_IP));
        info.setRpcPort(readIntField(base, ZkPath.RPC_PORT, 0));
        info.setEnable(readBoolField(base, ZkPath.ENABLE));
        info.setInServerList(readBoolField(base, ZkPath.IN_SERVER_LIST));
        info.setOpenTimeMs(readLongField(base, ZkPath.OPEN_TIME_MS, 0L));
        info.setRegistedRole(readLongField(base, ZkPath.REGISTED_ROLE, 0L));
        info.setDbVersion(readField(base, ZkPath.DB_VERSION));
        info.setTimeZoneOffset(readIntField(base, ZkPath.TIME_ZONE_OFFSET, 0));
        info.setMergeServerVersion(readIntField(base, ZkPath.MERGE_SERVER_VERSION, 0));
        info.setMultiRoleServerShow(readBoolField(base, ZkPath.MULTI_ROLE_SERVER_SHOW));
        info.setRedis(readRedisInfo(base));
        info.setMongo(readMongoInfo(base));
        info.setAlive(configService.exists(ZkPath.instancePath(base)));

        return info;
    }

    /**
     * 列出所有已注册的 GameServer ID
     *
     * @return 服务器ID列表
     */
    public List<Integer> listGameServerIds() {
        return listServerIds(ZkPath.GAME_SERVERS);
    }

    /**
     * 批量读取所有 GameServer 信息
     *
     * @return GameServerZkInfo 列表
     */
    public List<GameServerZkInfo> readAllGameServers() {
        List<Integer> ids = listGameServerIds();
        List<GameServerZkInfo> result = new ArrayList<>();
        for (int id : ids) {
            GameServerZkInfo info = readGameServer(id);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }

    // ========================= SceneServer 读写 =========================

    /**
     * 将 SceneServerZkInfo 逐字段写入 ZK 子节点
     *
     * @param info SceneServer 注册信息
     */
    public void writeSceneServer(SceneServerZkInfo info) {
        String base = ZkPath.sceneServerPath(info.getServerId());
        writeField(base, ZkPath.RPC_IP, info.getRpcIp());
        writeField(base, ZkPath.RPC_PORT, String.valueOf(info.getRpcPort()));
        writeField(base, ZkPath.BIND_GAME_ID, String.valueOf(info.getBindGameId()));
        writeField(base, ZkPath.ENABLE, String.valueOf(info.isEnable()));
        writeField(base, ZkPath.DB_VERSION, info.getDbVersion());
        writeField(base, ZkPath.TIME_ZONE_OFFSET, String.valueOf(info.getTimeZoneOffset()));

        writeRedisInfo(base, info.getRedis());
        writeMongoInfo(base, info.getMongo());

        writeField(base, ZkPath.SCENE_CONFIG_END_FLAG, ZkPath.SCENE_CONFIG_END_FLAG);
        LoggerUtil.debug("SceneServer serverId={} 信息写入 ZK 完成", info.getServerId());
    }

    /**
     * 从 ZK 子节点读取并组装为 SceneServerZkInfo
     *
     * @param serverId 服务器ID
     * @return SceneServerZkInfo，节点不存在返回 null
     */
    public SceneServerZkInfo readSceneServer(int serverId) {
        String base = ZkPath.sceneServerPath(serverId);
        if (!configService.exists(base)) {
            return null;
        }

        SceneServerZkInfo info = new SceneServerZkInfo();
        info.setServerId(serverId);
        info.setRpcIp(readField(base, ZkPath.RPC_IP));
        info.setRpcPort(readIntField(base, ZkPath.RPC_PORT, 0));
        info.setBindGameId(readIntField(base, ZkPath.BIND_GAME_ID, 0));
        info.setEnable(readBoolField(base, ZkPath.ENABLE));
        info.setDbVersion(readField(base, ZkPath.DB_VERSION));
        info.setTimeZoneOffset(readIntField(base, ZkPath.TIME_ZONE_OFFSET, 0));
        info.setRedis(readRedisInfo(base));
        info.setMongo(readMongoInfo(base));
        info.setAlive(configService.exists(ZkPath.instancePath(base)));

        return info;
    }

    /**
     * 列出所有已注册的 SceneServer ID
     *
     * @return 服务器ID列表
     */
    public List<Integer> listSceneServerIds() {
        return listServerIds(ZkPath.SCENE_SERVERS);
    }

    /**
     * 批量读取所有 SceneServer 信息
     *
     * @return SceneServerZkInfo 列表
     */
    public List<SceneServerZkInfo> readAllSceneServers() {
        List<Integer> ids = listSceneServerIds();
        List<SceneServerZkInfo> result = new ArrayList<>();
        for (int id : ids) {
            SceneServerZkInfo info = readSceneServer(id);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }

    // ========================= 通用方法 =========================

    /**
     * 更新服务器的单个字段
     *
     * @param serverId 服务器ID
     * @param type     服务器类型
     * @param field    字段名（ZkPath 中的常量）
     * @param value    字段值
     */
    public void updateField(int serverId, ServerType type, String field, String value) {
        String base = type.serverPath(serverId);
        writeField(base, field, value);
    }

    /**
     * 检查配置完成标记是否存在
     *
     * @param serverId 服务器ID
     * @param type     服务器类型
     * @return true 配置已完成
     */
    public boolean checkConfigEndFlag(int serverId, ServerType type) {
        String base = type.serverPath(serverId);
        String flagPath = ZkPath.childPath(base, type.getConfigEndFlag());
        return configService.exists(flagPath);
    }

    // ========================= 内部方法 =========================

    private void writeField(String basePath, String fieldName, String value) {
        String path = ZkPath.childPath(basePath, fieldName);
        configService.setConfig(path, value != null ? value : "");
    }

    private String readField(String basePath, String fieldName) {
        String path = ZkPath.childPath(basePath, fieldName);
        return configService.getConfig(path);
    }

    private int readIntField(String basePath, String fieldName, int defaultValue) {
        String value = readField(basePath, fieldName);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long readLongField(String basePath, String fieldName, long defaultValue) {
        String value = readField(basePath, fieldName);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean readBoolField(String basePath, String fieldName) {
        String value = readField(basePath, fieldName);
        return "true".equalsIgnoreCase(value != null ? value.trim() : "");
    }

    private void writeRedisInfo(String basePath, RedisZkInfo redis) {
        if (redis == null) {
            return;
        }
        String redisBase = ZkPath.childPath(basePath, ZkPath.REDIS);
        configService.setConfig(ZkPath.childPath(redisBase, ZkPath.REDIS_HOST),
                redis.getHost() != null ? redis.getHost() : "");
        configService.setConfig(ZkPath.childPath(redisBase, ZkPath.REDIS_PORT),
                String.valueOf(redis.getPort()));
        configService.setConfig(ZkPath.childPath(redisBase, ZkPath.REDIS_PASSWORD),
                redis.getPassword() != null ? redis.getPassword() : "");
    }

    private RedisZkInfo readRedisInfo(String basePath) {
        String redisBase = ZkPath.childPath(basePath, ZkPath.REDIS);
        if (!configService.exists(redisBase)) {
            return null;
        }
        RedisZkInfo redis = new RedisZkInfo();
        redis.setHost(configService.getConfig(ZkPath.childPath(redisBase, ZkPath.REDIS_HOST)));
        redis.setPort(parseIntSafe(
                configService.getConfig(ZkPath.childPath(redisBase, ZkPath.REDIS_PORT)), 0));
        redis.setPassword(configService.getConfig(ZkPath.childPath(redisBase, ZkPath.REDIS_PASSWORD)));
        return redis;
    }

    private void writeMongoInfo(String basePath, MongoZkInfo mongo) {
        if (mongo == null) {
            return;
        }
        String mongoBase = ZkPath.childPath(basePath, ZkPath.MONGO);
        configService.setConfig(ZkPath.childPath(mongoBase, ZkPath.MONGO_DB_NAME),
                mongo.getDbName() != null ? mongo.getDbName() : "");
        configService.setConfig(ZkPath.childPath(mongoBase, ZkPath.MONGO_URL),
                mongo.getUrl() != null ? mongo.getUrl() : "");
    }

    private MongoZkInfo readMongoInfo(String basePath) {
        String mongoBase = ZkPath.childPath(basePath, ZkPath.MONGO);
        if (!configService.exists(mongoBase)) {
            return null;
        }
        MongoZkInfo mongo = new MongoZkInfo();
        mongo.setDbName(configService.getConfig(ZkPath.childPath(mongoBase, ZkPath.MONGO_DB_NAME)));
        mongo.setUrl(configService.getConfig(ZkPath.childPath(mongoBase, ZkPath.MONGO_URL)));
        return mongo;
    }

    private List<Integer> listServerIds(String serversPath) {
        List<String> children = configService.getChildren(serversPath);
        if (children.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = new ArrayList<>();
        for (String child : children) {
            try {
                ids.add(Integer.parseInt(child));
            } catch (NumberFormatException e) {
                LoggerUtil.warn("ZK 服务器节点名称不是数字: {}/{}", serversPath, child);
            }
        }
        Collections.sort(ids);
        return ids;
    }

    private int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
