package com.slg.scene.scene.util;

import com.slg.common.util.RandomUtil;
import com.slg.scene.scene.aoi.model.AoiGrid;
import com.slg.scene.scene.aoi.model.GridContainer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.base.model.Scene;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * 场景位置查找工具类
 * <p>专注于在指定区域内查找满足条件的非阻挡区域，基于 AOI 网格与随机点位顺序，支持条件过滤与范围扩大查询。</p>
 *
 * <p><b>能力：</b></p>
 * <ul>
 *   <li><b>非阻挡区域查询</b>：将传入范围转化为 {@link AoiGrid} 组成的区域（可能略大于传入范围），在区域内找一块指定大小的无阻挡矩形，返回其左下角</li>
 *   <li><b>随机点位选取</b>：每次取点先随机选一个 grid，再从该 grid 用 {@link AoiGrid#nextRandomPosition()} 取一个点位；对范围有明确要求时可通过条件函数控制</li>
 *   <li><b>条件查询</b>：{@link Predicate}{@code <Position>} 对候选左下角过滤（如限定区域、避开某点等）</li>
 *   <li><b>范围扩大查询</b>：未找到时按 DETAIL 网格步长逐轮扩大，已检查过的 AoiGrid 直接跳过，仅对新格子搜索</li>
 * </ul>
 * <p>所有查询在 {@link GridLayer#DETAIL} 层级下进行，与 {@link MapNodeUtil} 一致。区域按整格对齐会产生一定范围误差，可接受。</p>
 * <p><b>范围传参方式（与 {@link MapNodeUtil} 一致）：</b></p>
 * <ul>
 *   <li><b>角点</b>：左下角 (x1,y1)、右上角 (x2,y2)</li>
 *   <li><b>左下角 + 长宽</b>：{@link Position} leftBottom，searchLength（搜索区 X 方向）、searchWidth（搜索区 Y 方向）</li>
 *   <li><b>中心点 + 正方形半边长</b>：{@link Position} center，searchHalfSide（中心向四边各延伸）</li>
 *   <li><b>中心点 + 矩形半宽半高</b>：{@link #findNonBlockRectFromCenterRect} / {@link #findNonBlockRectWithExpandFromCenterRect}，参数 center、searchHalfWidth、searchHalfHeight</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/7
 */
public final class MapPositionUtil {

    /** 默认查询层级（详细视图） */
    private static final GridLayer DEFAULT_LAYER = GridLayer.DETAIL;

    /** DETAIL 层级网格步长，用于范围扩大时每轮扩展量 */
    private static final int EXPAND_STEP = GridLayer.DETAIL.getGridLength();

    private MapPositionUtil() {
    }

    // ==================== 基础查询 ====================

    /**
     * 在指定矩形搜索区域内查找一块无阻挡矩形区域
     * <p>每次取点先随机选一个 grid 再取该 grid 的一个随机点位，找到第一块满足 needLength×needWidth 且全无阻挡的矩形即返回其左下角。</p>
     *
     * @param scene       场景
     * @param x1          搜索区域左下角 X
     * @param y1          搜索区域左下角 Y
     * @param x2          搜索区域右上角 X
     * @param y2          搜索区域右上角 Y
     * @param needLength  所需非阻挡区域长度（X 方向格子数）
     * @param needWidth   所需非阻挡区域宽度（Y 方向格子数）
     * @return 非阻挡区域左下角，若不存在则 empty
     */
    public static Optional<Position> findNonBlockRect(Scene scene, int x1, int y1, int x2, int y2,
                                                       int needLength, int needWidth) {
        return findNonBlockRect(scene, x1, y1, x2, y2, needLength, needWidth, null);
    }

    /**
     * 在指定矩形搜索区域内按条件查找一块无阻挡矩形区域
     * <p>仅当候选左下角满足 condition 且该矩形内全无阻挡时才返回。</p>
     *
     * @param scene       场景
     * @param x1          搜索区域左下角 X
     * @param y1          搜索区域左下角 Y
     * @param x2          搜索区域右上角 X
     * @param y2          搜索区域右上角 Y
     * @param needLength  所需非阻挡区域长度（X 方向格子数）
     * @param needWidth   所需非阻挡区域宽度（Y 方向格子数）
     * @param condition   对候选左下角的过滤条件，为 null 表示不限制
     * @return 非阻挡区域左下角，若不存在则 empty
     */
    public static Optional<Position> findNonBlockRect(Scene scene, int x1, int y1, int x2, int y2,
                                                       int needLength, int needWidth,
                                                       Predicate<Position> condition) {
        if (needLength <= 0 || needWidth <= 0) {
            return Optional.empty();
        }
        int mapWidth = scene.getMapData().getWidth();
        int mapHeight = scene.getMapData().getHeight();
        if (needLength > mapWidth || needWidth > mapHeight) {
            return Optional.empty();
        }
        int length = Math.max(0, x2 - x1);
        int width = Math.max(0, y2 - y1);
        if (length < needLength || width < needWidth) {
            return Optional.empty();
        }
        GridContainer container = scene.getMultiGridContainer().getGridContainer(DEFAULT_LAYER);
        List<AoiGrid> grids = container.getGridsInRect(Position.valueOf(scene, x1, y1), length, width);
        int totalAttempts = 0;
        for (AoiGrid g : grids) {
            totalAttempts += g.getRandomPositionCount();
        }
        if (totalAttempts == 0) {
            return Optional.empty();
        }
        AtomicReference<Optional<Position>> result = new AtomicReference<>(Optional.empty());
        for (int i = 0; i < totalAttempts; i++) {
            AoiGrid grid = grids.get(RandomUtil.nextIndex(grids.size()));
            Position pos = grid.nextRandomPosition();
            if (pos == null) {
                continue;
            }
            if (tryPositionAsNonBlock(scene, pos, needLength, needWidth, mapWidth, mapHeight, condition, result)) {
                return result.get();
            }
        }
        return result.get();
    }

    /**
     * 校验一个点位是否可作为无阻挡矩形的左下角，若满足则设置 result
     *
     * @return 是否已满足并设置 result
     */
    private static boolean tryPositionAsNonBlock(Scene scene, Position pos, int needL, int needW,
                                                  int mapWidth, int mapHeight, Predicate<Position> condition,
                                                  AtomicReference<Optional<Position>> result) {
        int px = pos.x();
        int py = pos.y();
        int maxPx = mapWidth - needL;
        int maxPy = mapHeight - needW;
        if (px < 0 || py < 0 || px > maxPx || py > maxPy) {
            return false;
        }
        if (condition != null && !condition.test(pos)) {
            return false;
        }
        if (!scene.getBlockContainer().isRectNonBlocking(px, py, needL, needW)) {
            return false;
        }
        result.set(Optional.of(Position.valueOf(scene, px, py)));
        return true;
    }

    /** 在矩形搜索区域内查找非阻挡区域（左下角 + 长宽） */
    public static Optional<Position> findNonBlockRect(Scene scene, Position leftBottom, int searchLength, int searchWidth,
                                                       int needLength, int needWidth) {
        return findNonBlockRect(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + searchLength, leftBottom.y() + searchWidth, needLength, needWidth);
    }

    /** 在矩形搜索区域内按条件查找非阻挡区域（左下角 + 长宽） */
    public static Optional<Position> findNonBlockRect(Scene scene, Position leftBottom, int searchLength, int searchWidth,
                                                       int needLength, int needWidth, Predicate<Position> condition) {
        return findNonBlockRect(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + searchLength, leftBottom.y() + searchWidth, needLength, needWidth, condition);
    }

    /** 在正方形搜索区域内查找非阻挡区域（中心点 + 半边长） */
    public static Optional<Position> findNonBlockRect(Scene scene, Position center, int searchHalfSide,
                                                       int needLength, int needWidth) {
        return findNonBlockRect(scene, center.x() - searchHalfSide, center.y() - searchHalfSide,
                center.x() + searchHalfSide, center.y() + searchHalfSide, needLength, needWidth);
    }

    /** 在正方形搜索区域内按条件查找非阻挡区域（中心点 + 半边长） */
    public static Optional<Position> findNonBlockRect(Scene scene, Position center, int searchHalfSide,
                                                       int needLength, int needWidth, Predicate<Position> condition) {
        return findNonBlockRect(scene, center.x() - searchHalfSide, center.y() - searchHalfSide,
                center.x() + searchHalfSide, center.y() + searchHalfSide, needLength, needWidth, condition);
    }

    /** 在矩形搜索区域内查找非阻挡区域（中心点 + 半宽半高，避免与「左下角+长宽」重载签名冲突故单独命名） */
    public static Optional<Position> findNonBlockRectFromCenterRect(Scene scene, Position center,
                                                                     int searchHalfWidth, int searchHalfHeight,
                                                                     int needLength, int needWidth) {
        return findNonBlockRect(scene, center.x() - searchHalfWidth, center.y() - searchHalfHeight,
                center.x() + searchHalfWidth, center.y() + searchHalfHeight, needLength, needWidth);
    }

    /** 在矩形搜索区域内按条件查找非阻挡区域（中心点 + 半宽半高） */
    public static Optional<Position> findNonBlockRectFromCenterRect(Scene scene, Position center,
                                                                     int searchHalfWidth, int searchHalfHeight,
                                                                     int needLength, int needWidth,
                                                                     Predicate<Position> condition) {
        return findNonBlockRect(scene, center.x() - searchHalfWidth, center.y() - searchHalfHeight,
                center.x() + searchHalfWidth, center.y() + searchHalfHeight, needLength, needWidth, condition);
    }

    // ==================== 范围扩大查询 ====================

    /**
     * 在指定矩形搜索区域内查找非阻挡区域，若未找到则逐轮扩大范围
     * <p>每轮向四周各扩一个 DETAIL 网格步长；已检查过的 AoiGrid 直接跳过，仅对新格子搜索。</p>
     *
     * @param scene           场景
     * @param x1              搜索区域左下角 X
     * @param y1              搜索区域左下角 Y
     * @param x2              搜索区域右上角 X
     * @param y2              搜索区域右上角 Y
     * @param needLength      所需非阻挡区域长度（X 方向格子数）
     * @param needWidth       所需非阻挡区域宽度（Y 方向格子数）
     * @param maxExpandRounds 最多扩大轮数（0 表示不扩大）
     * @return 非阻挡区域左下角，若不存在则 empty
     */
    public static Optional<Position> findNonBlockRectWithExpand(Scene scene, int x1, int y1, int x2, int y2,
                                                                 int needLength, int needWidth, int maxExpandRounds) {
        return findNonBlockRectWithExpand(scene, x1, y1, x2, y2, needLength, needWidth, maxExpandRounds, null);
    }

    /**
     * 在指定矩形搜索区域内按条件查找非阻挡区域，若未找到则逐轮扩大范围
     *
     * @param scene           场景
     * @param x1              搜索区域左下角 X
     * @param y1              搜索区域左下角 Y
     * @param x2              搜索区域右上角 X
     * @param y2              搜索区域右上角 Y
     * @param needLength      所需非阻挡区域长度（X 方向格子数）
     * @param needWidth       所需非阻挡区域宽度（Y 方向格子数）
     * @param maxExpandRounds 最多扩大轮数（0 表示不扩大）
     * @param condition       对候选左下角的过滤条件，为 null 表示不限制
     * @return 非阻挡区域左下角，若不存在则 empty
     */
    public static Optional<Position> findNonBlockRectWithExpand(Scene scene, int x1, int y1, int x2, int y2,
                                                                 int needLength, int needWidth, int maxExpandRounds,
                                                                 Predicate<Position> condition) {
        if (needLength <= 0 || needWidth <= 0) {
            return Optional.empty();
        }
        int mapWidth = scene.getMapData().getWidth();
        int mapHeight = scene.getMapData().getHeight();
        if (needLength > mapWidth || needWidth > mapHeight) {
            return Optional.empty();
        }
        GridContainer container = scene.getMultiGridContainer().getGridContainer(DEFAULT_LAYER);
        Set<AoiGrid> seenGrids = new HashSet<>();
        int curX1 = x1, curY1 = y1, curX2 = x2, curY2 = y2;
        AtomicReference<Optional<Position>> result = new AtomicReference<>(Optional.empty());
        for (int round = 0; round <= maxExpandRounds; round++) {
            int length = Math.max(0, curX2 - curX1);
            int width = Math.max(0, curY2 - curY1);
            if (length >= needLength && width >= needWidth) {
                List<AoiGrid> grids = container.getGridsInRect(Position.valueOf(scene, curX1, curY1), length, width);
                List<AoiGrid> newGrids = new ArrayList<>();
                for (AoiGrid g : grids) {
                    if (!seenGrids.contains(g)) {
                        newGrids.add(g);
                        seenGrids.add(g);
                    }
                }
                int totalAttempts = 0;
                for (AoiGrid g : newGrids) {
                    totalAttempts += g.getRandomPositionCount();
                }
                if (!newGrids.isEmpty() && totalAttempts > 0) {
                    for (int i = 0; i < totalAttempts; i++) {
                        AoiGrid grid = newGrids.get(RandomUtil.nextIndex(newGrids.size()));
                        Position pos = grid.nextRandomPosition();
                        if (pos == null) {
                            continue;
                        }
                        if (tryPositionAsNonBlock(scene, pos, needLength, needWidth, mapWidth, mapHeight, condition, result)) {
                            return result.get();
                        }
                    }
                }
            }
            curX1 -= EXPAND_STEP;
            curY1 -= EXPAND_STEP;
            curX2 += EXPAND_STEP;
            curY2 += EXPAND_STEP;
        }
        return Optional.empty();
    }

    /** 范围扩大查询（左下角 + 长宽） */
    public static Optional<Position> findNonBlockRectWithExpand(Scene scene, Position leftBottom,
                                                                 int searchLength, int searchWidth,
                                                                 int needLength, int needWidth, int maxExpandRounds) {
        return findNonBlockRectWithExpand(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + searchLength, leftBottom.y() + searchWidth,
                needLength, needWidth, maxExpandRounds, null);
    }

    /** 范围扩大查询（左下角 + 长宽，带条件） */
    public static Optional<Position> findNonBlockRectWithExpand(Scene scene, Position leftBottom,
                                                                 int searchLength, int searchWidth,
                                                                 int needLength, int needWidth, int maxExpandRounds,
                                                                 Predicate<Position> condition) {
        return findNonBlockRectWithExpand(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + searchLength, leftBottom.y() + searchWidth,
                needLength, needWidth, maxExpandRounds, condition);
    }

    /** 范围扩大查询（中心点 + 正方形半边长） */
    public static Optional<Position> findNonBlockRectWithExpand(Scene scene, Position center, int searchHalfSide,
                                                                 int needLength, int needWidth, int maxExpandRounds) {
        return findNonBlockRectWithExpand(scene, center.x() - searchHalfSide, center.y() - searchHalfSide,
                center.x() + searchHalfSide, center.y() + searchHalfSide,
                needLength, needWidth, maxExpandRounds, null);
    }

    /** 范围扩大查询（中心点 + 正方形半边长，带条件） */
    public static Optional<Position> findNonBlockRectWithExpand(Scene scene, Position center, int searchHalfSide,
                                                                 int needLength, int needWidth, int maxExpandRounds,
                                                                 Predicate<Position> condition) {
        return findNonBlockRectWithExpand(scene, center.x() - searchHalfSide, center.y() - searchHalfSide,
                center.x() + searchHalfSide, center.y() + searchHalfSide,
                needLength, needWidth, maxExpandRounds, condition);
    }

    /** 范围扩大查询（中心点 + 矩形半宽半高，避免与「左下角+长宽」重载签名冲突故单独命名） */
    public static Optional<Position> findNonBlockRectWithExpandFromCenterRect(Scene scene, Position center,
                                                                               int searchHalfWidth, int searchHalfHeight,
                                                                               int needLength, int needWidth,
                                                                               int maxExpandRounds) {
        return findNonBlockRectWithExpand(scene, center.x() - searchHalfWidth, center.y() - searchHalfHeight,
                center.x() + searchHalfWidth, center.y() + searchHalfHeight,
                needLength, needWidth, maxExpandRounds, null);
    }

    /** 范围扩大查询（中心点 + 矩形半宽半高，带条件） */
    public static Optional<Position> findNonBlockRectWithExpandFromCenterRect(Scene scene, Position center,
                                                                               int searchHalfWidth, int searchHalfHeight,
                                                                               int needLength, int needWidth,
                                                                               int maxExpandRounds,
                                                                               Predicate<Position> condition) {
        return findNonBlockRectWithExpand(scene, center.x() - searchHalfWidth, center.y() - searchHalfHeight,
                center.x() + searchHalfWidth, center.y() + searchHalfHeight,
                needLength, needWidth, maxExpandRounds, condition);
    }
}
