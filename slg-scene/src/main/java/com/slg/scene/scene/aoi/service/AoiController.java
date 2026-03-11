package com.slg.scene.scene.aoi.service;

import com.slg.common.executor.core.WorkerThreadPool;
import com.slg.common.util.CollectionUtil;
import com.slg.common.util.MathUtil;
import com.slg.common.util.TimeUtil;
import com.slg.net.message.clientmessage.scene.packet.SM_SceneArmyAppear;
import com.slg.net.message.clientmessage.scene.packet.SM_SceneArmyDisappear;
import com.slg.net.message.clientmessage.scene.packet.SM_SceneNodeAppear;
import com.slg.net.message.clientmessage.scene.packet.SM_SceneNodeDisappear;
import com.slg.scene.SpringContext;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.common.executor.Executor;
import com.slg.scene.net.ToClientPacketUtil;
import com.slg.scene.scene.aoi.model.AoiGrid;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.aoi.model.Watcher;
import com.slg.scene.scene.base.model.FPosition;
import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.component.impl.common.SelectTargetComponent;
import com.slg.scene.scene.node.node.model.RouteNode;
import com.slg.scene.scene.node.node.model.SceneNode;
import com.slg.scene.scene.node.node.service.NodeContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AOI（Area of Interest）控制器
 * <p>负责管理场景中的视野系统，实现玩家视野内对象的动态加载和卸载</p>
 *
 * <p><b>核心机制：</b></p>
 * <ul>
 *   <li><b>九宫格算法</b>：基于网格的空间划分，快速查找视野范围内的对象</li>
 *   <li><b>三步处理</b>：step1计算变化 → step2广播新增 → step3广播删除</li>
 *   <li><b>延迟删除</b>：对象离开视野后延迟4秒删除，避免频繁抖动</li>
 *   <li><b>批量广播</b>：按对象聚合玩家，减少网络消息数量</li>
 * </ul>
 *
 * <p><b>数据结构说明：</b></p>
 * <ul>
 *   <li><code>watchTasks</code>：待处理的视野更新任务，key=playerId，同一玩家多次提交会覆盖</li>
 *   <li><code>addNodePlayers</code>：新增对象的玩家列表，key=nodeId，value=需要看见该对象的玩家集合</li>
 *   <li><code>forgetNodePlayers</code>：待删除对象的玩家列表，key=nodeId，value=需要删除该对象的玩家集合</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
public class AoiController {

    /**
     * 所属场景
     */
    private final Scene scene;

    /**
     * 待处理的视野更新任务（key=playerId, value=任务）
     */
    private Map<Long, Runnable> watchTasks = new HashMap<>();

    /**
     * 新增节点的玩家映射（key=nodeId, value=需要看见该节点的玩家集合）
     */
    private Map<Long, Map<Long, Watcher>> addNodePlayers = new ConcurrentHashMap<>();

    /**
     * 待遗忘节点的玩家映射（key=nodeId, value=需要遗忘该节点的玩家集合）
     */
    private Map<Long, Map<Long, Watcher>> forgetNodePlayers = new ConcurrentHashMap<>();

    /**
     * 下次执行遗忘处理的时间戳
     */
    private long nextForgetTime = TimeUtil.now();

    /**
     * 遗忘延迟时间（4秒），避免频繁删除
     */
    private long forgetTime = TimeUtil.SECOND * 4;

    public AoiController(Scene scene){
        this.scene = scene;
    }

    /**
     * 提交视野更新任务
     * <p>当玩家镜头位置或缩放层级发生变化时，提交一个视野更新任务</p>
     * <p>任务会在下一个 tick 的 step1 阶段批量处理，避免频繁计算</p>
     * <p>同一个玩家的多次提交会覆盖之前的任务（保留最新的）</p>
     *
     * @param playerId  玩家ID
     * @param position  新的镜头中心位置
     * @param gridLayer 新的视野层级（缩放级别）
     */
    public void submitWatchTask(long playerId, Position position, GridLayer gridLayer){
        Executor.Scene.execute(() -> {
            watchTasks.put(playerId, () -> {
                aoiStep1(playerId, position, gridLayer);
            });
        });
    }

    /**
     * AOI tick 定时批量处理视野变化
     * <p><b>执行流程：</b></p>
     * <ol>
     *   <li><b>step1</b>：并行计算所有玩家的视野变化，整理新增和遗忘的 node</li>
     *   <li><b>step2</b>：按 node 聚合，批量广播新增消息（减少消息数量）</li>
     *   <li><b>step3</b>：每4秒执行一次，批量广播遗忘消息（延迟删除机制）</li>
     * </ol>
     * <p><b>优化策略：</b></p>
     * <ul>
     *   <li>延迟遗忘：避免玩家在视野边界频繁移动时的抖动</li>
     *   <li>批量广播：按 node 聚合 watcher，减少消息发送次数</li>
     *   <li>并行计算：step1/step2/step3 内部任务并行，step 之间串行</li>
     * </ul>
     */
    public void aoiTick(){
        WorkerThreadPool.getInstance().executeTasks(watchTasks.values());
        watchTasks.clear();
        List<Runnable> step2 = addNodePlayers.keySet().stream()
                .map(nodeId -> (Runnable) () -> aoiStep2(nodeId))
                .collect(Collectors.toList());
        WorkerThreadPool.getInstance().executeTasks(step2);
        addNodePlayers.clear();
        long now = TimeUtil.now();
        if (now >= nextForgetTime) {
            List<Runnable> step3 = forgetNodePlayers.keySet().stream()
                    .map(nodeId -> (Runnable) () -> aoiStep3(nodeId))
                    .collect(Collectors.toList());
            WorkerThreadPool.getInstance().executeTasks(step3);
            nextForgetTime = now + forgetTime;
            forgetNodePlayers.clear();
        }

    }

    /**
     * army tick 定时更新军队坐标（全静态场景下不需要这个tick）
     */
    public void armyTick(){
        NodeContainer nodeContainer = scene.getNodeContainer();
        List<Runnable> step3 = nodeContainer.getRouteNodes().values().stream()
                .map(routeNode -> (Runnable) () -> aoiArmy(routeNode))
                .collect(Collectors.toList());
        WorkerThreadPool.getInstance().executeTasks(step3);

    }


    /**
     * AOI step1：计算玩家视野变化
     * <p><b>主要功能：</b></p>
     * <ol>
     *   <li>获取玩家新位置的九宫格范围内的所有 node</li>
     *   <li>筛选出屏幕范围内的 node（精确筛选）</li>
     *   <li>对比旧视野，标记需要遗忘的 node</li>
     *   <li>对比旧视野，标记新增的 node</li>
     *   <li>处理"取消遗忘"的情况（之前标记遗忘，但现在又能看见）</li>
     * </ol>
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>遗忘的 node 不会立即删除，而是移到 forgetNodes 中，等待 step3 处理</li>
     *   <li>如果镜头位置和层级都未变化，直接跳过处理</li>
     *   <li>支持特殊 node（如玩家自己的城市）强制显示（待实现）</li>
     * </ul>
     *
     * @param playerId  玩家ID
     * @param position  新的镜头中心位置
     * @param gridLayer 新的视野层级
     */
    public void aoiStep1(long playerId, Position position, GridLayer gridLayer){
        Watcher watcher = scene.getWatcher(playerId);

        if (watcher == null) {
            return;
        }
        if (watcher.getPosition() != null && (watcher.getPosition().equals(position) && watcher.getGridLayer() == gridLayer)) {
            return;
        }

        AoiGrid grid = scene.getMultiGridContainer().getGridContainer(gridLayer).getGrid(position.x(), position.y());
        if (grid == null) {
            return;
        }
        // 更新 watcher 的位置和所在网格
        watcher.updatePosition(position, gridLayer);
        scene.getMultiGridContainer().updateWatcher(watcher);

        // ========== 第一步：收集新视野内的所有 node ==========
        Map<Long, SceneNode<?>> seeNodes = new HashMap<>(100);

        // 计算屏幕矩形范围（镜头中心 ± 半屏）
        int x1 = position.x() - gridLayer.getHalfScreenWidth(),
                x2 = position.x() + gridLayer.getHalfScreenWidth(),
                y1 = position.y() - gridLayer.getHalfScreenHeight(),
                y2 = position.y() + gridLayer.getHalfScreenHeight();

        // 遍历九宫格内的所有 node（粗筛选）
        for (AoiGrid nearGrid : grid.getNearGrids()) {
            for (SceneNode<?> node : nearGrid.getNodes().values()) {
                if (seeNodes.containsKey(node.getId())) {
                    continue; // 去重，避免九宫格重叠导致的重复
                }
                // 屏幕精确筛选：判断 node 是否在矩形范围内
                if (node.inRange(x1, y1, x2, y2)) {
                    seeNodes.put(node.getId(), node);
                }
            }
        }
        ScenePlayer scenePlayer = SpringContext.getScenePlayerManager().getScenePlayer(playerId);
        if (scenePlayer != null) {
            // todo 特殊处理一些必须要看到的 如自己的主城 自己的军队等, 塞进seeNodes里
            // 。。。
        }

        // todo 如果seeNodes超过了最大实体数量，在这里做优先级排序，然后截断超出范围的
        // 例如：按距离、重要性排序，只保留前 N 个
        // 。。。

        // ========== 第二步：对比旧视野，标记需要遗忘的 node ==========
        for (Map.Entry<Long, SceneNode<?>> entry : watcher.getSeeNodes().entrySet()) {
            Long nodeId = entry.getKey();
            if (seeNodes.containsKey(nodeId)) {
                continue; // 仍在新视野中，不需要遗忘
            }
            // 不在新视野中，标记为待遗忘（延迟删除）
            watcher.markForgetNode(entry.getValue());
            forgetNodePlayers.computeIfAbsent(nodeId, k -> new ConcurrentHashMap<>()).put(watcher.getId(), watcher);
        }

        // ========== 第三步：对比旧视野，处理新增和取消遗忘的 node ==========
        for (Map.Entry<Long, SceneNode<?>> entry : seeNodes.entrySet()) {
            Long nodeId = entry.getKey();
            if (watcher.getSeeNodes().containsKey(nodeId)) {
                // 已经在看见列表中，无需处理
                continue;
            }
            SceneNode<?> node = entry.getValue();
            if (watcher.getForgetNodes().containsKey(nodeId)) {
                // 取消遗忘：之前标记为待删除，但现在又能看见了
                watcher.cancelForgetNode(node);
                Map<Long, Watcher> forgetMap = forgetNodePlayers.get(nodeId);
                if (forgetMap != null) {
                    forgetMap.remove(watcher.getId()); // 从待删除列表移除
                }
                continue;
            }
            // 新增：之前没看见过，现在新看见的
            watcher.seeNode(node);
            addNodePlayers.computeIfAbsent(nodeId, k -> new ConcurrentHashMap<>()).put(watcher.getId(), watcher);
        }
    }

    /**
     * AOI step2：广播 node 新增消息
     * <p><b>优化说明：</b></p>
     * <ul>
     *   <li>按 node 聚合所有需要看见它的 watcher</li>
     *   <li>一次广播发送给所有相关玩家，减少消息数量</li>
     *   <li>如果 node 已被销毁或 watcher 列表为空，跳过广播</li>
     * </ul>
     *
     * @param nodeId 场景节点ID
     */
    public void aoiStep2(Long nodeId){
        SceneNode<?> sceneNode = scene.getNodeContainer().getSceneNodes().get(nodeId);
        if (sceneNode == null) {
            return;
        }

        SM_SceneNodeAppear sm = SM_SceneNodeAppear.valueOf(sceneNode.toVO());
        Map<Long, Watcher> watchers = addNodePlayers.get(nodeId);

        // 广播
        if (CollectionUtil.isNotBlank(watchers)) {
            ToClientPacketUtil.broadcast(watchers.keySet(), sm);
        }

    }

    /**
     * AOI step3：广播 node 删除消息
     * <p><b>延迟删除机制：</b></p>
     * <ul>
     *   <li>每4秒执行一次，批量处理所有待遗忘的 node</li>
     *   <li>避免玩家在视野边界频繁移动时反复发送出现/消失消息</li>
     *   <li>清理 watcher 的 forgetNodes 状态</li>
     * </ul>
     * <p><b>特殊处理：</b></p>
     * <ul>
     *   <li>如果 node 已被销毁，直接清理 watcher 的 forgetNodes，仍然发送消失消息</li>
     *   <li>确保客户端状态与服务端一致</li>
     * </ul>
     *
     * @param nodeId 场景节点ID
     */
    public void aoiStep3(Long nodeId){
        SceneNode<?> sceneNode = scene.getNodeContainer().getSceneNodes().get(nodeId);
        SM_SceneNodeDisappear sm = SM_SceneNodeDisappear.valueOf(nodeId);
        ;
        Map<Long, Watcher> watchers = forgetNodePlayers.get(nodeId);
        // 广播
        if (CollectionUtil.isNotBlank(watchers)) {
            for (Watcher watcher : watchers.values()) {
                if (sceneNode == null) {
                    watcher.getForgetNodes().remove(nodeId);
                } else{
                    watcher.forgetNode(sceneNode);
                }
            }
            ToClientPacketUtil.broadcast(watchers.keySet(), sm);
        }
    }

    /**
     * 军队实体无视野裁剪 独立模块设计 便于后期拆除 完全使用tick来控制army的出现和移除，方便后期去掉
     * 1. 通过SelectTargetComponent获取当前坐标, 获取当前AOIGrid
     * 2. 检索旧的watcher 移除军队
     * 3. 检索新的watcher 增加军队
     *
     * @param routeNode
     */
    public void aoiArmy(RouteNode<?> routeNode){

        SelectTargetComponent selectTargetComponent = routeNode.getComponent(ComponentEnum.SelectTarget);
        if (selectTargetComponent == null) {
            return;
        }
        Position position = selectTargetComponent.getCurrentPosition().toPosition();
        Set<Long> forgetPlayers = new HashSet<>();
        Map<Long, Watcher> addPlayers = new HashMap<>();
        for (Watcher watcher : routeNode.getSeeArmyList().values()) {
            if (!routeNode.getSeeMeList().containsKey(watcher.getId())) {
                // 线已经看不到了，军队不用管了
                routeNode.getSeeArmyList().remove(watcher.getId());
                continue;
            }
            GridLayer gridLayer = watcher.getGridLayer();
            Position watcherPosition = watcher.getPosition();
            int x1 = watcherPosition.x() - gridLayer.getHalfScreenWidth(),
                    x2 = watcherPosition.x() + gridLayer.getHalfScreenWidth(),
                    y1 = watcherPosition.y() - gridLayer.getHalfScreenHeight(),
                    y2 = watcherPosition.y() + gridLayer.getHalfScreenHeight();
            if (MathUtil.pointInRect(position.x(), position.y(), x1, y1, x2, y2)) {
                // 仍然能看到
            } else{
                // 看不到了
                forgetPlayers.add(watcher.getId());
            }
        }

        for (Watcher watcher : routeNode.getSeeMeList().values()) {
            if (routeNode.getSeeArmyList().containsKey(watcher.getId())) {
                continue;
            }
            GridLayer gridLayer = watcher.getGridLayer();
            int x1 = position.x() - gridLayer.getHalfScreenWidth(),
                    x2 = position.x() + gridLayer.getHalfScreenWidth(),
                    y1 = position.y() - gridLayer.getHalfScreenHeight(),
                    y2 = position.y() + gridLayer.getHalfScreenHeight();
            if (MathUtil.pointInRect(position.x(), position.y(), x1, y1, x2, y2)) {
                // 新进入视野
                addPlayers.put(watcher.getId(), watcher);
            }
        }

        if (CollectionUtil.isNotBlank(forgetPlayers)) {
            forgetPlayers.forEach(id -> routeNode.getSeeArmyList().remove(id));
            ToClientPacketUtil.broadcast(forgetPlayers, SM_SceneArmyDisappear.valueOf(routeNode.getId()));
        }

        if (CollectionUtil.isNotBlank(addPlayers)) {
            routeNode.getSeeArmyList().putAll(addPlayers);
            ToClientPacketUtil.broadcast(addPlayers.keySet(), SM_SceneArmyAppear.valueOf(routeNode.getId()));
        }


    }


}
