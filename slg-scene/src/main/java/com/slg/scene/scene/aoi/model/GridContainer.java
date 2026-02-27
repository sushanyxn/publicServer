package com.slg.scene.scene.aoi.model;

import com.slg.common.util.MathUtil;
import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.base.model.FPosition;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.node.node.model.SceneNode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 单层网格容器
 * <p>管理某一特定视野层级的所有网格</p>
 * 
 * <p><b>主要功能：</b></p>
 * <ul>
 *   <li>初始化网格数组，划分地图空间</li>
 *   <li>建立网格的九宫格邻居关系</li>
 *   <li>提供坐标到网格的快速查找</li>
 *   <li>管理节点和观察者在网格中的分布</li>
 * </ul>
 * 
 * <p><b>网格划分规则：</b></p>
 * <ul>
 *   <li>根据 gridLength 将地图划分为 gridWidthNum * gridHeightNum 个网格</li>
 *   <li>网格索引计算：index = y * gridWidthNum + x</li>
 *   <li>地图边界不计入网格（mapWidth = width - 1）</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
public class GridContainer {

    /** 所属场景 */
    private Scene scene;


    /** 视野层级 */
    private GridLayer gridLayer;
    
    /** 网格边长（决定网格划分的粒度） */
    private final int gridLength;
    
    /** 网格数组（一维数组存储二维网格） */
    private final AoiGrid[] grids;
    
    /** 地图横向网格数量 */
    private final int gridWidthNum;
    
    /** 地图纵向网格数量 */
    private final int gridHerightNum;
    
    /** 地图实际宽度（不包括右边界） */
    private final int mapWidth;
    
    /** 地图实际高度（不包括上边界） */
    private final int mapHeight;

    public GridContainer(Scene scene, GridLayer layer){

        this.scene = scene;
        this.gridLayer = layer;
        this.gridLength = layer.getGridLength();
        // 地图右上角边界不算格子
        MapData mapData = scene.getMapData();
        this.mapWidth = mapData.getWidth() - 1;
        this.mapHeight = mapData.getHeight() - 1;

        // 计算网格数量，注意补足余数部分
        this.gridWidthNum = mapWidth / gridLength + 1;
        this.gridHerightNum = mapHeight / gridLength + 1;

        grids = new AoiGrid[gridWidthNum * gridHerightNum];

        // 初始化视野网格
        for (int i = 0; i < gridWidthNum; i++) {
            for (int j = 0; j < gridHerightNum; j++) {
                int x = i * gridLength;
                int y = j * gridLength;
                AoiGrid grid = new AoiGrid(x, y, this);
                grid.initRandomPositions(gridLength);
                grids[getIndex(x, y)] = grid;
            }
        }

        // 初始化临近格子
        for (AoiGrid grid : grids) {
            List<AoiGrid> nears = new ArrayList<>(9);
            nears.add(grid);
            // 寻找附近的九宫格
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    AoiGrid near = getGrid(grid.getX() + i * gridLength, grid.getY() + j * gridLength);
                    if (near == null || near == grid) {
                        continue;
                    }
                    nears.add(near);
                }
            }
            grid.setNearGrids(nears.toArray(new AoiGrid[0]));
        }
    }

    /**
     * 根据世界坐标计算网格索引
     * <p><b>计算步骤：</b></p>
     * <ol>
     *   <li>检查坐标是否在地图范围内</li>
     *   <li>将世界坐标转换为网格坐标（除以 gridLength）</li>
     *   <li>计算一维索引：index = y * gridWidthNum + x</li>
     * </ol>
     *
     * @param x 世界坐标 X
     * @param y 世界坐标 Y
     * @return 网格索引，超出范围返回 -1
     */
    public int getIndex(int x, int y){
        if (x < 0 || y < 0 || x > mapWidth || y > mapHeight) {
            return -1;
        }
        x /= gridLength;
        y /= gridLength;
        return y * gridWidthNum + x;
    }

    /**
     * 根据世界坐标获取网格
     * <p>快速定位某个坐标点所在的网格</p>
     *
     * @param x 世界坐标 X
     * @param y 世界坐标 Y
     * @return 对应的网格，超出范围返回 null
     */
    public AoiGrid getGrid(int x, int y){
        int index = getIndex(x, y);
        if (index < 0 || index >= grids.length) {
            return null;
        }
        return grids[index];
    }

    /**
     * 将场景节点添加到网格
     * <p>委托给节点自己处理，节点根据自身位置决定进入哪些网格</p>
     *
     * @param sceneNode 场景节点
     */
    public void addSceneNode(SceneNode<?> sceneNode){
        sceneNode.enterGrid(this);
    }

    /**
     * 将场景节点从网格移除
     * <p>委托给节点自己处理，节点从所在的网格中移除</p>
     *
     * @param sceneNode 场景节点
     */
    public void removeSceneNode(SceneNode<?> sceneNode){
        sceneNode.exitGrid(this);
    }

    /**
     * 将观察者添加到网格
     * <p>根据观察者当前位置，将其添加到对应网格</p>
     *
     * @param watcher 观察者
     */
    public void addWatcher(Watcher watcher){
        Position position = watcher.getPosition();
        AoiGrid grid = getGrid(position.x(), position.y());
        if (grid != null) {
            grid.addWatcher(watcher);
        }
    }

    /**
     * 将观察者从网格移除
     * <p>根据观察者当前位置，将其从对应网格移除</p>
     *
     * @param watcher 观察者
     */
    public void removeWatcher(Watcher watcher){
        Position position = watcher.getPosition();
        AoiGrid grid = getGrid(position.x(), position.y());
        if (grid != null) {
            grid.removeWatcher(watcher);
        }
    }

    /**
     * 遍历两点之间直线经过的所有网格（按网格步进的 DDA 算法）
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>军队移动：计算路径上的所有网格</li>
     *   <li>技能范围：计算直线技能影响的网格</li>
     *   <li>视线检测：判断两点之间是否有遮挡</li>
     * </ul>
     * <p>步数等于穿过的网格数，与线段像素长度解耦，长线段下性能更稳定。</p>
     *
     * @param start        起点坐标
     * @param end          终点坐标
     * @param foreachTask  对每个网格执行的操作
     */
    public void foreachLineGrid(Position start, Position end, Consumer<AoiGrid> foreachTask){
        int x0 = start.x();
        int y0 = start.y();
        int x1 = end.x();
        int y1 = end.y();
        int L = gridLength;
        MathUtil.forEachGridOnSegment(x0, y0, x1, y1, L, (gx, gy) -> {
            AoiGrid grid = getGrid(gx * L, gy * L);
            if (grid != null) {
                foreachTask.accept(grid);
            }
        });
    }

    /**
     * 遍历两点之间直线经过的所有网格（FPosition 版本，按格子坐标参与网格计算）
     */
    public void foreachLineGrid(FPosition start, FPosition end, Consumer<AoiGrid> foreachTask){
        int x0 = start.gridX();
        int y0 = start.gridY();
        int x1 = end.gridX();
        int y1 = end.gridY();
        int L = gridLength;
        MathUtil.forEachGridOnSegment(x0, y0, x1, y1, L, (gx, gy) -> {
            AoiGrid grid = getGrid(gx * L, gy * L);
            if (grid != null) {
                foreachTask.accept(grid);
            }
        });
    }

    /**
     * 遍历矩形区域覆盖的所有网格
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>静态节点（建筑、资源点等）：矩形占位进入/离开网格</li>
     *   <li>视野范围：计算矩形区域内涉及的网格</li>
     * </ul>
     * <p>矩形为左下角 leftBottom，向右延伸 length、向上延伸 width。</p>
     *
     * @param leftBottom  矩形左下角坐标
     * @param length      矩形长度（X 方向）
     * @param width       矩形宽度（Y 方向）
     * @param foreachTask 对每个网格执行的操作
     */
    public void foreachRectGrid(Position leftBottom, int length, int width, Consumer<AoiGrid> foreachTask) {
        int x0 = leftBottom.x();
        int y0 = leftBottom.y();
        int x1 = x0 + length;
        int y1 = y0 + width;
        int L = gridLength;
        int gxMin = (x0 / L) * L;
        int gyMin = (y0 / L) * L;
        int gxMax = (x1 / L) * L;
        int gyMax = (y1 / L) * L;
        for (int gx = gxMin; gx <= gxMax; gx += L) {
            for (int gy = gyMin; gy <= gyMax; gy += L) {
                AoiGrid grid = getGrid(gx, gy);
                if (grid != null) {
                    foreachTask.accept(grid);
                }
            }
        }
    }

    /**
     * 获取矩形区域覆盖的所有网格
     * <p>将传入的矩形范围转化为与矩形相交的 AoiGrid 列表，转化后的区域可能大于传入范围（按整格对齐）。</p>
     *
     * @param leftBottom 矩形左下角坐标
     * @param length     矩形长度（X 方向）
     * @param width      矩形宽度（Y 方向）
     * @return 与矩形相交的网格列表，不含 null
     */
    public List<AoiGrid> getGridsInRect(Position leftBottom, int length, int width) {
        List<AoiGrid> result = new ArrayList<>();
        foreachRectGrid(leftBottom, length, width, result::add);
        return result;
    }

}
