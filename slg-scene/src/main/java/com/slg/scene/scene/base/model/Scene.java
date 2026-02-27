package com.slg.scene.scene.base.model;

import com.slg.net.message.clientmessage.scene.packet.SM_SceneNodeAppear;
import com.slg.net.message.clientmessage.scene.packet.SM_SceneNodeDisappear;
import com.slg.scene.net.ToClientPacketUtil;
import com.slg.scene.scene.aoi.model.MapData;
import com.slg.scene.scene.aoi.service.AoiController;
import com.slg.scene.scene.aoi.service.MultiGridContainer;
import com.slg.scene.scene.aoi.model.Watcher;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.component.impl.common.BlockComponent;
import com.slg.scene.scene.node.node.model.SceneNode;
import com.slg.scene.scene.node.node.service.NodeContainer;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景实体
 * 代表游戏中的一个场景
 *
 * @author yangxunan
 * @date 2026/1/23
 */
@Getter
@Setter
public class Scene {

    /**
     * 场景ID
     */
    private long sceneId;

    /**
     * 场景配置id
     */
    private int sceneConfigId;

    /**
     * 地图数据
     */
    private MapData mapData;

    /**
     * 网格数据
     */
    private MultiGridContainer multiGridContainer;

    /**
     * node 容器
     */
    private NodeContainer nodeContainer;

    /**
     * 阻挡容器（地形 + 动态节点占格）
     */
    private BlockContainer blockContainer;

    /**
     * aoi 控制器
     */
    private AoiController aoiController;

    public Scene(long sceneId, int sceneConfigId) {
        this.sceneId = sceneId;
        this.sceneConfigId = sceneConfigId;
    }

    /**
     * 读取地图文件
     *
     * @param fileName
     */
    public void loadData(String fileName){
        mapData = new MapData();
        multiGridContainer = new MultiGridContainer(this);
        nodeContainer = new NodeContainer(this);
        blockContainer = new BlockContainer(this);
        aoiController = new AoiController(this);
    }

    /**
     * 出生在场景上
     *
     * @param sceneNode
     */
    public void spawn(SceneNode<?> sceneNode){
        if (sceneNode.isSpawned()) {
            return;
        }

        // 如果有阻挡组件，判断是否有阻挡
        BlockComponent<?> blockComponent = sceneNode.getComponent(ComponentEnum.Block);
        if (blockComponent != null && !blockComponent.blockCheck(this)) {
            // 存在阻挡无法出生
            return;
        }

        // 添加到node容器
        nodeContainer.addSceneNode(sceneNode);
        // 添加到视野网格
        multiGridContainer.addSceneNode(sceneNode);
        sceneNode.setSpawned(true);

        if (blockComponent != null) {
            // 在场景中添加自己的阻挡
            blockComponent.blockAdd(this);
        }

        // 广播
        SM_SceneNodeAppear sm = SM_SceneNodeAppear.valueOf(sceneNode.toVO());
        ToClientPacketUtil.broadcast(sceneNode.getSeeMeList().keySet(), sm);

    }

    /**
     * 从场景上移除
     *
     * @param sceneNode
     */
    public void despawn(SceneNode<?> sceneNode){
        if (!sceneNode.isSpawned()) {
            return;
        }

        sceneNode.setSpawned(false);
        nodeContainer.removeSceneNode(sceneNode);
        multiGridContainer.removeSceneNode(sceneNode);

        // 释放阻挡
        BlockComponent<?> blockComponent = sceneNode.getComponent(ComponentEnum.Block);
        if (blockComponent != null) {
            blockComponent.blockRemove(this);
        }

        SM_SceneNodeDisappear sm = SM_SceneNodeDisappear.valueOf(sceneNode.getId());
        ToClientPacketUtil.broadcast(sceneNode.getSeeMeList().keySet(), sm);
        sceneNode.getSeeMeList().values().forEach(watcher -> {
            watcher.forgetNode(sceneNode);
        });

    }

    public void enter(Watcher watcher){
        multiGridContainer.updateWatcher(watcher);
    }

    public void exit(long playerId){
        multiGridContainer.removeWatcher(playerId);
    }

    public Watcher getWatcher(long playerId){
        return multiGridContainer.getWatchers().get(playerId);
    }

}
