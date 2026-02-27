package com.slg.net.zookeeper.service;

import com.slg.common.log.LoggerUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Zookeeper 配置读取服务
 * 基于 CuratorFramework 提供配置节点的读写和监听功能
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class ZookeeperConfigService {

    private final CuratorFramework curatorFramework;

    /** 已注册的 CuratorCache 缓存（用于关闭时清理） */
    private final Map<String, CuratorCache> cacheMap = new ConcurrentHashMap<>();

    public ZookeeperConfigService(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    /**
     * 读取节点数据
     *
     * @param path 节点路径（相对于 basePath）
     * @return 节点数据字符串，节点不存在返回 null
     */
    public String getConfig(String path) {
        try {
            byte[] data = curatorFramework.getData().forPath(path);
            return data != null ? new String(data, StandardCharsets.UTF_8) : null;
        } catch (Exception e) {
            LoggerUtil.error("读取 Zookeeper 配置失败, path={}", path, e);
            return null;
        }
    }

    /**
     * 写入节点数据（节点不存在则自动创建）
     *
     * @param path 节点路径（相对于 basePath）
     * @param data 节点数据
     * @return true 写入成功
     */
    public boolean setConfig(String path, String data) {
        try {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            if (exists(path)) {
                curatorFramework.setData().forPath(path, bytes);
            } else {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(path, bytes);
            }
            LoggerUtil.debug("写入 Zookeeper 配置成功, path={}", path);
            return true;
        } catch (Exception e) {
            LoggerUtil.error("写入 Zookeeper 配置失败, path={}", path, e);
            return false;
        }
    }

    /**
     * 监听配置变更（含初始事件）
     * 使用 CuratorCache 监听指定路径下的节点变化
     *
     * @param path     节点路径（相对于 basePath）
     * @param listener 变更回调，参数为 (path, newData)，newData 在删除事件时为 null
     */
    public void watchConfig(String path, BiConsumer<String, String> listener) {
        watchConfig(path, listener, false);
    }

    /**
     * 监听配置变更
     * 使用 CuratorCache 监听指定路径下的节点变化
     *
     * @param path               节点路径（相对于 basePath）
     * @param listener           变更回调，参数为 (path, newData)，newData 在删除事件时为 null
     * @param skipInitialEvents  为 true 时忽略 CuratorCache 启动时触发的初始 create 事件，
     *                           仅在 forInitialized 回调触发后才开始处理后续真实变更
     */
    public void watchConfig(String path, BiConsumer<String, String> listener, boolean skipInitialEvents) {
        try {
            AtomicBoolean cacheInitialized = new AtomicBoolean(!skipInitialEvents);

            CuratorCache cache = CuratorCache.build(curatorFramework, path);
            CuratorCacheListener cacheListener = CuratorCacheListener.builder()
                    .forCreates(node -> {
                        if (!cacheInitialized.get()) {
                            return;
                        }
                        String nodeData = new String(node.getData(), StandardCharsets.UTF_8);
                        listener.accept(node.getPath(), nodeData);
                    })
                    .forChanges((oldNode, newNode) -> {
                        if (!cacheInitialized.get()) {
                            return;
                        }
                        String nodeData = new String(newNode.getData(), StandardCharsets.UTF_8);
                        listener.accept(newNode.getPath(), nodeData);
                    })
                    .forDeletes(node -> {
                        if (!cacheInitialized.get()) {
                            return;
                        }
                        listener.accept(node.getPath(), null);
                    })
                    .forInitialized(() -> {
                        cacheInitialized.set(true);
                        LoggerUtil.debug("CuratorCache 初始数据加载完成, path={}", path);
                    })
                    .build();

            cache.listenable().addListener(cacheListener);
            cache.start();
            cacheMap.put(path, cache);

            LoggerUtil.debug("注册 Zookeeper 配置监听器, path={}", path);
        } catch (Exception e) {
            LoggerUtil.error("注册 Zookeeper 配置监听器失败, path={}", path, e);
        }
    }

    /**
     * 取消配置监听
     *
     * @param path 节点路径
     */
    public void unwatchConfig(String path) {
        CuratorCache cache = cacheMap.remove(path);
        if (cache != null) {
            cache.close();
            LoggerUtil.debug("取消 Zookeeper 配置监听器, path={}", path);
        }
    }

    /**
     * 取消所有配置监听，关闭全部 CuratorCache
     */
    public void unwatchAll() {
        for (Map.Entry<String, CuratorCache> entry : cacheMap.entrySet()) {
            entry.getValue().close();
            LoggerUtil.debug("取消 Zookeeper 配置监听器, path={}", entry.getKey());
        }
        cacheMap.clear();
    }

    /**
     * 获取子节点列表
     *
     * @param path 父节点路径（相对于 basePath）
     * @return 子节点名称列表，失败返回空列表
     */
    public List<String> getChildren(String path) {
        try {
            return curatorFramework.getChildren().forPath(path);
        } catch (Exception e) {
            LoggerUtil.error("获取 Zookeeper 子节点失败, path={}", path, e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查节点是否存在
     *
     * @param path 节点路径（相对于 basePath）
     * @return true 节点存在
     */
    public boolean exists(String path) {
        try {
            Stat stat = curatorFramework.checkExists().forPath(path);
            return stat != null;
        } catch (Exception e) {
            LoggerUtil.error("检查 Zookeeper 节点是否存在失败, path={}", path, e);
            return false;
        }
    }

    /**
     * 删除节点（递归删除子节点）
     *
     * @param path 节点路径（相对于 basePath）
     * @return true 删除成功
     */
    public boolean deleteConfig(String path) {
        try {
            if (exists(path)) {
                curatorFramework.delete()
                        .deletingChildrenIfNeeded()
                        .forPath(path);
                LoggerUtil.debug("删除 Zookeeper 节点成功, path={}", path);
                return true;
            }
            return false;
        } catch (Exception e) {
            LoggerUtil.error("删除 Zookeeper 节点失败, path={}", path, e);
            return false;
        }
    }
}
