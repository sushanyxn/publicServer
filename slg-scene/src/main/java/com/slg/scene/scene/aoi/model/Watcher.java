package com.slg.scene.scene.aoi.model;

import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.node.node.model.SceneNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 观察者（Watcher）- 玩家视野管理器
 * <p>每个在线玩家对应一个 Watcher 对象，负责管理该玩家的视野状态</p>
 * 
 * <p><b>三状态设计：</b></p>
 * <ul>
 *   <li><b>seeNodes</b>：玩家当前能看见的节点（客户端已加载）</li>
 *   <li><b>forgetNodes</b>：准备遗忘的节点（等待删除，延迟4秒）</li>
 *   <li><b>无</b>：完全看不见的节点（客户端未加载）</li>
 * </ul>
 * 
 * <p><b>状态转换：</b></p>
 * <pre>
 * 无 --[seeNode()]--> seeNodes --[markForgetNode()]--> forgetNodes --[forgetNode()]--> 无
 *                         ^                                  |
 *                         |----------[cancelForgetNode()]----
 * </pre>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
@Setter
public class Watcher {

    /** 玩家ID */
    private long id;

    /** 所属game进程ID */
    private int gameServerId;

    /** 当前视野中心位置 */
    private Position position;

    /** 当前视野层级（缩放级别） */
    private GridLayer gridLayer = GridLayer.LAYER1;

    /** 当前看见的节点集合（客户端已加载的对象） */
    private Map<Long, SceneNode<?>> seeNodes = new ConcurrentHashMap<>();

    /** 待遗忘的节点集合（等待删除，延迟4秒） */
    private Map<Long, SceneNode<?>> forgetNodes = new ConcurrentHashMap<>();

    public Watcher(ScenePlayer scenePlayer){
        this.id = scenePlayer.getId();
        this.gameServerId = scenePlayer.getGameServerId();
    }

    /**
     * 更新视野位置和层级
     * <p>当玩家镜头移动或缩放时调用</p>
     *
     * @param position   新的镜头中心位置
     * @param gridLayer  新的视野层级
     */
    public void updatePosition(Position position, GridLayer gridLayer){
        this.position = position;
        this.gridLayer = gridLayer;
    }

    /**
     * 看见新节点（状态转换：无 → seeNodes 或 forgetNodes → seeNodes）
     * <p><b>操作：</b></p>
     * <ol>
     *   <li>将节点加入 seeNodes</li>
     *   <li>将自己加入节点的观察者列表</li>
     *   <li>从 forgetNodes 中移除（防御性清理）</li>
     * </ol>
     *
     * @param sceneNode 场景节点
     */
    public void seeNode(SceneNode<?> sceneNode){
        seeNodes.put(sceneNode.getId(), sceneNode);
        sceneNode.getSeeMeList().put(id, this);
        forgetNodes.remove(sceneNode.getId());
    }

    /**
     * 标记节点为待遗忘（状态转换：seeNodes → forgetNodes）
     * <p><b>延迟删除机制：</b></p>
     * <ul>
     *   <li>节点不会立即删除，而是等待 step3 批量处理</li>
     *   <li>如果玩家在4秒内又移回来，可以取消遗忘</li>
     * </ul>
     *
     * @param sceneNode 场景节点
     */
    public void markForgetNode(SceneNode<?> sceneNode){
        seeNodes.remove(sceneNode.getId());
        forgetNodes.put(sceneNode.getId(), sceneNode);
    }

    /**
     * 取消遗忘节点（状态转换：forgetNodes → seeNodes）
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>节点已标记为待遗忘，但玩家又移回视野内</li>
     *   <li>避免发送"消失→出现"的抖动消息</li>
     * </ul>
     *
     * @param sceneNode 场景节点
     */
    public void cancelForgetNode(SceneNode<?> sceneNode){
        forgetNodes.remove(sceneNode.getId());
        seeNodes.put(sceneNode.getId(), sceneNode);
    }

    /**
     * 完全遗忘节点（状态转换：forgetNodes → 无）
     * <p><b>操作：</b></p>
     * <ol>
     *   <li>从 forgetNodes 中移除</li>
     *   <li>从节点的观察者列表中移除自己</li>
     * </ol>
     * <p><b>注意：</b>不操作 seeNodes，因为节点已经在 markForgetNode 时移除</p>
     *
     * @param sceneNode 场景节点
     */
    public void forgetNode(SceneNode<?> sceneNode){
        seeNodes.remove(sceneNode.getId());
        forgetNodes.remove(sceneNode.getId());
        sceneNode.getSeeMeList().remove(id);
    }

    /**
     * 清空所有视野状态
     * <p>玩家离线时调用，清理所有节点的观察者列表</p>
     */
    public void clearAll(){
        for (SceneNode<?> node : seeNodes.values()) {
            node.getSeeMeList().remove(id);
        }
        for (SceneNode<?> node : forgetNodes.values()) {
            node.getSeeMeList().remove(id);
        }
        seeNodes.clear();
        forgetNodes.clear();
    }

}
