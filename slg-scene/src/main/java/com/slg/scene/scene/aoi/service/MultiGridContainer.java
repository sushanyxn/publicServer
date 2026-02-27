package com.slg.scene.scene.aoi.service;

import com.slg.scene.scene.aoi.model.GridContainer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.aoi.model.Watcher;
import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.node.node.model.SceneNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多层级网格容器
 * <p>管理所有视野层级的网格容器，支持多缩放级别</p>
 * 
 * <p><b>多层级设计目的：</b></p>
 * <ul>
 *   <li><b>性能优化</b>：远距离视野使用大网格，减少计算量</li>
 *   <li><b>LOD机制</b>：不同距离显示不同详细度的对象</li>
 *   <li><b>灵活切换</b>：客户端缩放时切换到对应层级</li>
 * </ul>
 * 
 * <p><b>节点分层规则：</b></p>
 * <ul>
 *   <li>每个节点属于一个或多个层级（通过 belongHighLayer 判断）</li>
 *   <li>例如：重要建筑在所有层级都显示，普通装饰只在 DETAIL 层显示</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
@Setter
public class MultiGridContainer {

    /** 各层级的网格容器数组 */
    private GridContainer[] gridContainers;

    /** 场景中所有观察者的映射表（key=playerId） */
    private Map<Long, Watcher> watchers = new ConcurrentHashMap<>();

    public  MultiGridContainer(Scene scene) {
        this.gridContainers = new GridContainer[GridLayer.VALUES.length];
        for (GridLayer gridLayer : GridLayer.VALUES) {
            gridContainers[gridLayer.ordinal()] = new  GridContainer(scene, gridLayer);
        }
    }

    /**
     * 获取指定层级的网格容器
     *
     * @param gridLayer 视野层级
     * @return 对应的网格容器
     */
    public GridContainer getGridContainer(GridLayer gridLayer) {
        return gridContainers[gridLayer.ordinal()];
    }

    /**
     * 更新观察者的网格位置
     * <p>当玩家镜头移动或切换层级时，需要：</p>
     * <ol>
     *   <li>从旧网格中移除</li>
     *   <li>添加到新网格</li>
     * </ol>
     *
     * @param watcher 观察者
     */
    public void updateWatcher(Watcher watcher) {
        removeWatcher(watcher.getId());
        getGridContainer(watcher.getGridLayer()).addWatcher(watcher);
    }

    /**
     * 移除观察者
     * <p>玩家离线时调用</p>
     *
     * @param playerId 玩家ID
     */
    public void removeWatcher(long playerId) {
        Watcher watcher = watchers.remove(playerId);
        if (watcher != null) {
            getGridContainer(watcher.getGridLayer()).removeWatcher(watcher);
        }
    }

    /**
     * 添加场景节点到所有适用的层级
     * <p><b>分层规则：</b></p>
     * <ul>
     *   <li>节点的 belongHighLayer 决定了它能在哪些层级显示</li>
     *   <li>例如：belongHighLayer = LAYER2，则在 DETAIL、LAYER1、LAYER2 都显示</li>
     * </ul>
     *
     * @param sceneNode 场景节点
     */
    public void addSceneNode(SceneNode<?> sceneNode) {
        for (GridLayer layer : GridLayer.VALUES) {
            if (belongLayer(sceneNode, layer)) {
                getGridContainer(layer).addSceneNode(sceneNode);
            }
        }
    }

    /**
     * 从所有层级移除场景节点
     *
     * @param sceneNode 场景节点
     */
    public void removeSceneNode(SceneNode<?> sceneNode) {
        for (GridLayer layer : GridLayer.VALUES) {
            if (belongLayer(sceneNode, layer)) {
                getGridContainer(layer).removeSceneNode(sceneNode);
            }
        }
    }

    /**
     * 判断节点是否属于某个层级
     * <p><b>判断规则：</b></p>
     * <ul>
     *   <li>节点的最高层级 >= 目标层级：属于</li>
     *   <li>例如：节点最高层级=LAYER2，则属于 DETAIL、LAYER1、LAYER2</li>
     * </ul>
     *
     * @param sceneNode 场景节点
     * @param gridLayer 目标层级
     * @return true=属于，false=不属于
     */
    private boolean belongLayer(SceneNode<?> sceneNode, GridLayer gridLayer) {
        return sceneNode.belongHighLayer().ordinal() >= gridLayer.ordinal();
    }
}
