package com.slg.scene.scene.aoi.model;

import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.node.node.model.SceneNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AOI 网格单元
 * <p>九宫格算法的基本单元，负责管理一个网格范围内的节点和观察者</p>
 * 
 * <p><b>九宫格算法：</b></p>
 * <ul>
 *   <li>将地图划分为多个网格</li>
 *   <li>每个网格记录其九宫格范围内的邻居网格</li>
 *   <li>查询视野时只需遍历当前网格的九宫格，无需遍历整个地图</li>
 * </ul>
 * 
 * <p><b>网格坐标：</b></p>
 * <ul>
 *   <li>x, y 表示网格左下角的世界坐标</li>
 *   <li>例如：网格长度=16，则网格(0,0)覆盖世界坐标[0-15, 0-15]</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
@Setter
public class AoiGrid {

    /** 网格左下角的世界坐标 X */
    private int x;

    /** 网格左下角的世界坐标 Y */
    private int y;

    /** 九宫格邻居引用（包括自己），用于快速查找视野范围 */
    private AoiGrid[] nearGrids;

    /** 网格内的场景节点（城市、军队等游戏对象） */
    private Map<Long, SceneNode<?>> nodes = new ConcurrentHashMap<>();

    /** 网格内的观察者（当前镜头中心在此网格的玩家） */
    private Map<Long, Watcher> watchers = new ConcurrentHashMap<>();

    /** 所属的网格容器（用于反向查询） */
    private GridContainer gridContainer;

    /**
     * 本网格内所有坐标点的随机顺序数组，用于随机点位选取
     * <p>覆盖 [x, x+gridLength) × [y, y+gridLength) 的每个整数坐标，初始化时打乱顺序。</p>
     */
    private Position[] randomPositions;

    /**
     * 随机点位选取的当前下标，每次 {@link #nextRandomPosition()} 后自增并取模，实现轮转
     */
    private final AtomicInteger randomPositionIndex = new AtomicInteger(0);

    public AoiGrid(int x, int y, GridContainer gridContainer){
        this.x = x;
        this.y = y;
        this.gridContainer = gridContainer;
    }

    /**
     * 初始化并打乱本网格内的随机点位数组
     * <p>在 GridContainer 构造时调用，生成 [x, x+gridLength) × [y, y+gridLength) 内所有坐标点并打乱顺序。</p>
     *
     * @param gridLength 网格边长（与本网格覆盖范围一致）
     */
    public void initRandomPositions(int gridLength) {
        int size = gridLength * gridLength;
        Position[] positions = new Position[size];
        int idx = 0;
        for (int dy = 0; dy < gridLength; dy++) {
            for (int dx = 0; dx < gridLength; dx++) {
                positions[idx++] = Position.valueOf(gridContainer.getScene(), this.x + dx, this.y + dy);
            }
        }
        List<Position> list = Arrays.asList(positions);
        Collections.shuffle(list);
        this.randomPositions = list.toArray(new Position[0]);
    }

    /**
     * 取下一个随机点位并更新下标
     * <p>按初始化时打乱的顺序轮转返回网格内坐标，每次调用将下标推进一位（到末尾后从 0 重新开始）。</p>
     *
     * @return 下一个点位，若未初始化 randomPositions 或为空则返回 null
     */
    public Position nextRandomPosition() {
        Position[] arr = randomPositions;
        if (arr == null || arr.length == 0) {
            return null;
        }
        int cur = randomPositionIndex.getAndUpdate(i -> (i + 1) % arr.length);
        return arr[cur];
    }

    /**
     * 本网格内随机点位的数量（用于单轮搜索时控制尝试次数）
     *
     * @return 点位数量，未初始化时为 0
     */
    public int getRandomPositionCount() {
        return randomPositions == null ? 0 : randomPositions.length;
    }

    /**
     * 添加观察者到网格
     * <p>当玩家镜头中心移动到此网格时调用</p>
     *
     * @param watcher 观察者
     */
    public void addWatcher(Watcher watcher){
        watchers.put(watcher.getId(), watcher);
    }

    /**
     * 从网格移除观察者
     * <p>当玩家镜头中心移动离开此网格时调用</p>
     *
     * @param watcher 观察者
     */
    public void removeWatcher(Watcher watcher){
        watchers.remove(watcher.getId());
    }

    /**
     * 添加场景节点到网格
     * <p>节点进入场景或移动到此网格时调用</p>
     *
     * @param sceneNode 场景节点
     */
    public void addSceneNode(SceneNode<?> sceneNode){
        nodes.put(sceneNode.getId(), sceneNode);

        for (Watcher watcher : watchers.values()) {
            Position position = watcher.getPosition();
            GridLayer gridLayer = watcher.getGridLayer();
            // 计算屏幕矩形范围（镜头中心 ± 半屏）
            int x1 = position.x() - gridLayer.getHalfScreenWidth(),
                    x2 = position.x() + gridLayer.getHalfScreenWidth(),
                    y1 = position.y() - gridLayer.getHalfScreenHeight(),
                    y2 = position.y() + gridLayer.getHalfScreenHeight();
            if (sceneNode.inRange(x1, y1, x2, y2)) {
                watcher.seeNode(sceneNode);
            }
        }

    }

    /**
     * 从网格移除场景节点
     * <p>节点离开场景或移动离开此网格时调用</p>
     *
     * @param sceneNode 场景节点
     */
    public void removeSceneNode(SceneNode<?> sceneNode){
        nodes.remove(sceneNode.getId());
    }


}
