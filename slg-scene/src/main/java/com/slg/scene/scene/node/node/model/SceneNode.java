package com.slg.scene.scene.node.node.model;

import com.slg.net.message.clientmessage.scene.packet.SceneNodeVO;
import com.slg.scene.scene.aoi.model.GridContainer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.aoi.model.Watcher;
import com.slg.scene.scene.node.component.ComponentContainer;
import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.owner.NodeOwner;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场景节点基类
 * <p>场景中所有可见对象的抽象基类（城市、军队、资源点等）</p>
 * 
 * <p><b>核心功能：</b></p>
 * <ul>
 *   <li><b>组件系统</b>：通过组件扩展节点功能（位置、移动、战斗等）</li>
 *   <li><b>观察者追踪</b>：记录哪些玩家能看见此节点（用于主动推送）</li>
 *   <li><b>多层级支持</b>：节点可在不同缩放层级显示</li>
 *   <li><b>协议转换</b>：将节点数据转换为客户端协议</li>
 * </ul>
 * 
 * <p><b>生命周期：</b></p>
 * <ol>
 *   <li>创建节点 → initComponents()</li>
 *   <li>进入场景 → enterGrid() → spawned=true</li>
 *   <li>玩家看见 → 加入 seeMeList</li>
 *   <li>离开场景 → exitGrid() → 清理 seeMeList</li>
 * </ol>
 *
 * @param <T> 节点拥有者类型（玩家、NPC等）
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
@Setter
public abstract class SceneNode<T extends NodeOwner> {

    /** 节点唯一ID */
    protected long id;

    /** 节点拥有者（城市属于某个玩家，资源点可能无主） */
    protected T owner;

    /** 组件容器（存储节点的各种组件） */
    protected ComponentContainer componentContainer = new ComponentContainer();

    /** 是否已出生（是否已加入场景） */
    protected volatile boolean spawned;

    /** 观察者列表（哪些玩家看见了此节点） */
    protected Map<Long, Watcher> seeMeList = new ConcurrentHashMap<>();

    public SceneNode(T owner, long id) {
        this.id = id;
        this.owner = owner;
    }

    public <C extends AbstractNodeComponent<?>> C getComponent(ComponentEnum componentEnum) {
        return componentContainer.getComponent(componentEnum);
    }

    public <C extends AbstractNodeComponent<?>> void registerComponent(C component) {
        componentContainer.registerComponent(component);
    }

    /**
     * 初始化节点组件
     * <p>子类重写此方法，添加所需的组件（如位置组件、移动组件等）</p>
     */
    public abstract void initComponents();

    /**
     * 转换为客户端协议对象
     * <p>将节点数据转换为客户端可识别的 VO 对象</p>
     *
     * @return 场景节点VO
     */
    public abstract SceneNodeVO toVO();

    /**
     * 进入视野网格
     * <p>节点加入场景时调用，将自己添加到对应的网格中</p>
     * <p><b>注意：</b>一个节点可能占据多个网格（例如大型建筑）</p>
     *
     * @param gridContainer 网格容器
     */
    public abstract void enterGrid(GridContainer gridContainer);

    /**
     * 离开视野网格
     * <p>节点离开场景时调用，从所有网格中移除</p>
     *
     * @param gridContainer 网格容器
     */
    public abstract void exitGrid(GridContainer gridContainer);

    /**
     * 返回节点所属的最高视野层级
     * <p><b>分层规则：</b></p>
     * <ul>
     *   <li>返回 DETAIL：只在详细视图显示（小型装饰）</li>
     *   <li>返回 LAYER1：在标准及以下层级显示（普通建筑）</li>
     *   <li>返回 LAYER3：在所有层级显示（重要建筑、城市）</li>
     * </ul>
     *
     * @return 最高层级
     */
    public abstract GridLayer belongHighLayer();

    /**
     * 判断节点是否在指定矩形范围内
     * <p>用于视野筛选，判断节点是否在玩家屏幕范围内</p>
     *
     * @param x1 矩形左下角X
     * @param y1 矩形左下角Y
     * @param x2 矩形右上角X
     * @param y2 矩形右上角Y
     * @return true=在范围内, false=不在范围内
     */
    public abstract boolean inRange(int x1, int y1, int x2, int y2);
}
