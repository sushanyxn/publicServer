package com.slg.scene.scene.util;

import com.slg.scene.scene.aoi.model.AoiGrid;
import com.slg.scene.scene.aoi.model.GridContainer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.base.model.Scene;
import com.slg.scene.scene.node.node.model.SceneNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 场景节点查找工具类
 * <p>专注于 node 相关的查找，基于 AOI 网格在指定范围内查找节点，支持条件过滤、类型多态与最优单结果查询。</p>
 *
 * <p><b>能力：</b></p>
 * <ul>
 *   <li><b>范围查询</b>：将传入范围转化为 {@link AoiGrid} 组成的区域（可能略大于传入范围），按网格筛选节点，避免全图遍历</li>
 *   <li><b>条件查询</b>：{@link Predicate} 过滤；对距离有明确要求时可在条件中自行判断</li>
 *   <li><b>多态</b>：按 {@link Class} 限定返回类型（如只查 RouteNode、StaticNode）</li>
 *   <li><b>最优查询</b>：在区域内按 {@link Comparator} 取最优一个，或按距离取最近一个</li>
 *   <li><b>范围扩大查询</b>：结果不足时按 DETAIL 网格步长逐轮扩大，已检查过的 AoiGrid 直接跳过，仅对新格子收集并合并到已有结果</li>
 * </ul>
 * <p>所有查询默认在 {@link GridLayer#DETAIL} 层级下进行。区域按整格对齐会产生一定范围误差，可接受；精确距离由条件函数控制。</p>
 * <p><b>范围传参方式：</b></p>
 * <ul>
 *   <li><b>角点</b>：左下角 (x1,y1)、右上角 (x2,y2)</li>
 *   <li><b>左下角 + 长宽</b>：{@link Position} leftBottom，length（X 方向）、width（Y 方向）</li>
 *   <li><b>中心点 + 正方形半边长</b>：{@link Position} center，halfSide（中心向四边各延伸 halfSide）</li>
 *   <li><b>中心点 + 矩形半宽半高</b>：center，halfWidth、halfHeight（用于矩形或最近点查询）</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/7
 */
public final class MapNodeUtil {

    /** 默认查询层级（详细视图） */
    private static final GridLayer DEFAULT_LAYER = GridLayer.DETAIL;

    /** DETAIL 层级网格步长，用于范围扩大时每轮扩展量 */
    private static final int EXPAND_STEP = GridLayer.DETAIL.getGridLength();

    private MapNodeUtil() {
    }

    // ==================== 基础范围查询 ====================

    /**
     * 在矩形范围内查找所有节点
     *
     * @param scene 场景
     * @param x1    矩形左下角 X
     * @param y1    矩形左下角 Y
     * @param x2    矩形右上角 X
     * @param y2    矩形右上角 Y
     * @return 范围内的节点列表（去重）
     */
    public static List<SceneNode<?>> findNodesInRect(Scene scene, int x1, int y1, int x2, int y2) {
        return findNodesInRect(scene, x1, y1, x2, y2, (Predicate<SceneNode<?>>) null);
    }

    /**
     * 在矩形范围内按条件查找节点
     *
     * @param scene  场景
     * @param x1     矩形左下角 X
     * @param y1     矩形左下角 Y
     * @param x2     矩形右上角 X
     * @param y2     矩形右上角 Y
     * @param filter 条件过滤，为 null 表示不过滤
     * @return 满足条件的节点列表（去重）
     */
    public static List<SceneNode<?>> findNodesInRect(Scene scene, int x1, int y1, int x2, int y2,
                                                     Predicate<SceneNode<?>> filter) {
        Map<Long, SceneNode<?>> acc = new LinkedHashMap<>();
        collectInto(scene, x1, y1, x2, y2, null, filter, acc);
        return new ArrayList<>(acc.values());
    }

    /**
     * 在矩形范围内查找指定类型的节点（多态）
     *
     * @param scene    场景
     * @param x1       矩形左下角 X
     * @param y1       矩形左下角 Y
     * @param x2       矩形右上角 X
     * @param y2       矩形右上角 Y
     * @param nodeType 节点类型（如 RouteNode.class、StaticNode.class）
     * @param <N>      节点泛型
     * @return 范围内且类型匹配的节点列表（去重）
     */
    public static <N extends SceneNode<?>> List<N> findNodesInRect(Scene scene, int x1, int y1, int x2, int y2,
                                                                   Class<N> nodeType) {
        return findNodesInRect(scene, x1, y1, x2, y2, nodeType, null);
    }

    /**
     * 在矩形范围内按类型 + 条件查找节点（多态 + 条件）
     *
     * @param scene    场景
     * @param x1       矩形左下角 X
     * @param y1       矩形左下角 Y
     * @param x2       矩形右上角 X
     * @param y2       矩形右上角 Y
     * @param nodeType 节点类型
     * @param filter   条件过滤，为 null 表示不过滤
     * @param <N>      节点泛型
     * @return 满足类型与条件的节点列表（去重）
     */
    @SuppressWarnings("unchecked")
    public static <N extends SceneNode<?>> List<N> findNodesInRect(Scene scene, int x1, int y1, int x2, int y2,
                                                                   Class<N> nodeType, Predicate<N> filter) {
        Map<Long, SceneNode<?>> acc = new LinkedHashMap<>();
        collectInto(scene, x1, y1, x2, y2, nodeType, wrapFilter(nodeType, filter), acc);
        return (List<N>) new ArrayList<>(acc.values());
    }

    /** 在矩形范围内查找所有节点（左下角 + 长宽） */
    public static List<SceneNode<?>> findNodesInRect(Scene scene, Position leftBottom, int length, int width) {
        return findNodesInRect(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width);
    }

    /** 在矩形范围内按条件查找节点（左下角 + 长宽） */
    public static List<SceneNode<?>> findNodesInRect(Scene scene, Position leftBottom, int length, int width,
                                                     Predicate<SceneNode<?>> filter) {
        return findNodesInRect(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, filter);
    }

    /** 在矩形范围内查找指定类型节点（左下角 + 长宽） */
    public static <N extends SceneNode<?>> List<N> findNodesInRect(Scene scene, Position leftBottom, int length, int width,
                                                                   Class<N> nodeType) {
        return findNodesInRect(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, nodeType);
    }

    /** 在矩形范围内按类型+条件查找节点（左下角 + 长宽） */
    public static <N extends SceneNode<?>> List<N> findNodesInRect(Scene scene, Position leftBottom, int length, int width,
                                                                   Class<N> nodeType, Predicate<N> filter) {
        return findNodesInRect(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, nodeType, filter);
    }

    /** 在正方形范围内查找所有节点（中心点 + 半边长） */
    public static List<SceneNode<?>> findNodesInRect(Scene scene, Position center, int halfSide) {
        return findNodesInRect(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide);
    }

    /** 在正方形范围内按条件查找节点（中心点 + 半边长） */
    public static List<SceneNode<?>> findNodesInRect(Scene scene, Position center, int halfSide,
                                                     Predicate<SceneNode<?>> filter) {
        return findNodesInRect(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, filter);
    }

    /** 在正方形范围内查找指定类型节点（中心点 + 半边长） */
    public static <N extends SceneNode<?>> List<N> findNodesInRect(Scene scene, Position center, int halfSide,
                                                                   Class<N> nodeType) {
        return findNodesInRect(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, nodeType);
    }

    /** 在正方形范围内按类型+条件查找节点（中心点 + 半边长） */
    public static <N extends SceneNode<?>> List<N> findNodesInRect(Scene scene, Position center, int halfSide,
                                                                   Class<N> nodeType, Predicate<N> filter) {
        return findNodesInRect(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, nodeType, filter);
    }

    /**
     * 在矩形范围内按比较器取最优的一个节点
     *
     * @param scene      场景
     * @param x1         矩形左下角 X
     * @param y1         矩形左下角 Y
     * @param x2         矩形右上角 X
     * @param y2         矩形右上角 Y
     * @param filter     条件过滤，为 null 表示不过滤
     * @param comparator 比较器，越小越优
     * @return 最优节点，若无则 empty
     */
    public static Optional<SceneNode<?>> findBestInRect(Scene scene, int x1, int y1, int x2, int y2,
                                                        Predicate<SceneNode<?>> filter,
                                                        Comparator<SceneNode<?>> comparator) {
        return findNodesInRect(scene, x1, y1, x2, y2, filter).stream().min(comparator);
    }

    /** 在矩形范围内取最优节点（左下角 + 长宽） */
    public static Optional<SceneNode<?>> findBestInRect(Scene scene, Position leftBottom, int length, int width,
                                                        Predicate<SceneNode<?>> filter,
                                                        Comparator<SceneNode<?>> comparator) {
        return findBestInRect(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, filter, comparator);
    }

    /** 在正方形范围内取最优节点（中心点 + 半边长） */
    public static Optional<SceneNode<?>> findBestInRect(Scene scene, Position center, int halfSide,
                                                        Predicate<SceneNode<?>> filter,
                                                        Comparator<SceneNode<?>> comparator) {
        return findBestInRect(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, filter, comparator);
    }

    /**
     * 在矩形范围内取距离某点最近的一个节点
     *
     * @param scene          场景
     * @param center         中心点
     * @param halfWidth      矩形半宽（中心 ± halfWidth 为范围）
     * @param halfHeight     矩形半高
     * @param filter         条件过滤，为 null 表示不过滤
     * @param positionGetter 从节点得到代表坐标，若返回 null 则跳过该节点
     * @return 距离 center 最近的节点，若无则 empty
     */
    public static Optional<SceneNode<?>> findNearestInRect(Scene scene, Position center,
                                                           int halfWidth, int halfHeight,
                                                           Predicate<SceneNode<?>> filter,
                                                           Function<SceneNode<?>, Position> positionGetter) {
        List<SceneNode<?>> list = findNodesInRect(scene,
                center.x() - halfWidth, center.y() - halfHeight,
                center.x() + halfWidth, center.y() + halfHeight, filter);
        return pickNearest(list, center, positionGetter);
    }

    /** 在正方形范围内取距离中心最近的节点（中心点 + 半边长） */
    public static Optional<SceneNode<?>> findNearestInRect(Scene scene, Position center, int halfSide,
                                                           Predicate<SceneNode<?>> filter,
                                                           Function<SceneNode<?>, Position> positionGetter) {
        return findNearestInRect(scene, center, halfSide, halfSide, filter, positionGetter);
    }

    /**
     * 在矩形范围内找到第一个满足条件的节点（顺序不保证，仅语义为"任取一"）
     *
     * @param scene  场景
     * @param x1     矩形左下角 X
     * @param y1     矩形左下角 Y
     * @param x2     矩形右上角 X
     * @param y2     矩形右上角 Y
     * @param filter 条件过滤，为 null 表示任意节点
     * @return 第一个匹配的节点，若无则 empty
     */
    public static Optional<SceneNode<?>> findFirstInRect(Scene scene, int x1, int y1, int x2, int y2,
                                                         Predicate<SceneNode<?>> filter) {
        List<SceneNode<?>> list = findNodesInRect(scene, x1, y1, x2, y2, filter);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** 在矩形范围内任取一节点（左下角 + 长宽） */
    public static Optional<SceneNode<?>> findFirstInRect(Scene scene, Position leftBottom, int length, int width,
                                                         Predicate<SceneNode<?>> filter) {
        return findFirstInRect(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, filter);
    }

    /** 在正方形范围内任取一节点（中心点 + 半边长） */
    public static Optional<SceneNode<?>> findFirstInRect(Scene scene, Position center, int halfSide,
                                                         Predicate<SceneNode<?>> filter) {
        return findFirstInRect(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, filter);
    }

    // ==================== 范围扩大查询 ====================

    /**
     * 在矩形范围内查找节点，若结果不足 minCount 则逐轮扩大范围
     * <p>扩大策略：每轮向四周各扩一个 DETAIL 网格步长；已检查过的 AoiGrid 直接跳过，仅对新格子收集并合并到已有结果。</p>
     *
     * @param scene           场景
     * @param x1              矩形左下角 X
     * @param y1              矩形左下角 Y
     * @param x2              矩形右上角 X
     * @param y2              矩形右上角 Y
     * @param minCount        至少需要的节点个数
     * @param maxExpandRounds 最多扩大轮数（0 表示不扩大）
     * @return 满足条件的节点列表（去重）
     */
    public static List<SceneNode<?>> findNodesInRectWithExpand(Scene scene, int x1, int y1, int x2, int y2,
                                                               int minCount, int maxExpandRounds) {
        return findNodesInRectWithExpand(scene, x1, y1, x2, y2, minCount, maxExpandRounds, null);
    }

    /** 范围扩大查询（左下角 + 长宽） */
    public static List<SceneNode<?>> findNodesInRectWithExpand(Scene scene, Position leftBottom, int length, int width,
                                                               int minCount, int maxExpandRounds) {
        return findNodesInRectWithExpand(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, minCount, maxExpandRounds, null);
    }

    /** 范围扩大查询（中心点 + 正方形半边长） */
    public static List<SceneNode<?>> findNodesInRectWithExpand(Scene scene, Position center, int halfSide,
                                                               int minCount, int maxExpandRounds) {
        return findNodesInRectWithExpand(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, minCount, maxExpandRounds, null);
    }

    /**
     * 在矩形范围内按条件查找节点，若结果不足 minCount 则逐轮扩大范围
     *
     * @param scene           场景
     * @param x1              矩形左下角 X
     * @param y1              矩形左下角 Y
     * @param x2              矩形右上角 X
     * @param y2              矩形右上角 Y
     * @param minCount        至少需要的节点个数
     * @param maxExpandRounds 最多扩大轮数（0 表示不扩大）
     * @param filter          条件过滤，为 null 表示不过滤
     * @return 满足条件的节点列表（去重）
     */
    public static List<SceneNode<?>> findNodesInRectWithExpand(Scene scene, int x1, int y1, int x2, int y2,
                                                               int minCount, int maxExpandRounds,
                                                               Predicate<SceneNode<?>> filter) {
        return new ArrayList<>(expandCollect(scene, x1, y1, x2, y2, maxExpandRounds,
                null, filter, acc -> acc.size() >= minCount).values());
    }

    /** 范围扩大查询（左下角 + 长宽，带条件） */
    public static List<SceneNode<?>> findNodesInRectWithExpand(Scene scene, Position leftBottom, int length, int width,
                                                               int minCount, int maxExpandRounds,
                                                               Predicate<SceneNode<?>> filter) {
        return findNodesInRectWithExpand(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, minCount, maxExpandRounds, filter);
    }

    /** 范围扩大查询（中心点 + 正方形半边长，带条件） */
    public static List<SceneNode<?>> findNodesInRectWithExpand(Scene scene, Position center, int halfSide,
                                                               int minCount, int maxExpandRounds,
                                                               Predicate<SceneNode<?>> filter) {
        return findNodesInRectWithExpand(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, minCount, maxExpandRounds, filter);
    }

    /**
     * 在矩形范围内按类型 + 条件查找节点，若结果不足 minCount 则逐轮扩大范围
     *
     * @param scene           场景
     * @param x1              矩形左下角 X
     * @param y1              矩形左下角 Y
     * @param x2              矩形右上角 X
     * @param y2              矩形右上角 Y
     * @param minCount        至少需要的节点个数
     * @param maxExpandRounds 最多扩大轮数（0 表示不扩大）
     * @param nodeType        节点类型
     * @param filter          条件过滤，为 null 表示不过滤
     * @param <N>             节点泛型
     * @return 满足类型与条件的节点列表（去重）
     */
    @SuppressWarnings("unchecked")
    public static <N extends SceneNode<?>> List<N> findNodesInRectWithExpand(Scene scene,
                                                                            int x1, int y1, int x2, int y2,
                                                                            int minCount, int maxExpandRounds,
                                                                            Class<N> nodeType, Predicate<N> filter) {
        return (List<N>) new ArrayList<>(expandCollect(scene, x1, y1, x2, y2, maxExpandRounds,
                nodeType, wrapFilter(nodeType, filter), acc -> acc.size() >= minCount).values());
    }

    /** 范围扩大查询（左下角 + 长宽，按类型+条件） */
    public static <N extends SceneNode<?>> List<N> findNodesInRectWithExpand(Scene scene,
                                                                            Position leftBottom, int length, int width,
                                                                            int minCount, int maxExpandRounds,
                                                                            Class<N> nodeType, Predicate<N> filter) {
        return findNodesInRectWithExpand(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, minCount, maxExpandRounds, nodeType, filter);
    }

    /** 范围扩大查询（中心点 + 正方形半边长，按类型+条件） */
    public static <N extends SceneNode<?>> List<N> findNodesInRectWithExpand(Scene scene,
                                                                            Position center, int halfSide,
                                                                            int minCount, int maxExpandRounds,
                                                                            Class<N> nodeType, Predicate<N> filter) {
        return findNodesInRectWithExpand(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, minCount, maxExpandRounds, nodeType, filter);
    }

    /**
     * 在矩形范围内按比较器取最优的一个节点，若无结果则逐轮扩大范围
     *
     * @param scene           场景
     * @param x1              矩形左下角 X
     * @param y1              矩形左下角 Y
     * @param x2              矩形右上角 X
     * @param y2              矩形右上角 Y
     * @param maxExpandRounds 最多扩大轮数（0 表示不扩大）
     * @param filter          条件过滤，为 null 表示不过滤
     * @param comparator      比较器，越小越优
     * @return 最优节点，若无则 empty
     */
    public static Optional<SceneNode<?>> findBestInRectWithExpand(Scene scene, int x1, int y1, int x2, int y2,
                                                                  int maxExpandRounds,
                                                                  Predicate<SceneNode<?>> filter,
                                                                  Comparator<SceneNode<?>> comparator) {
        Map<Long, SceneNode<?>> acc = expandCollect(scene, x1, y1, x2, y2, maxExpandRounds,
                null, filter, m -> !m.isEmpty());
        return acc.values().stream().min(comparator);
    }

    /** 范围扩大取最优（左下角 + 长宽） */
    public static Optional<SceneNode<?>> findBestInRectWithExpand(Scene scene, Position leftBottom, int length, int width,
                                                                  int maxExpandRounds,
                                                                  Predicate<SceneNode<?>> filter,
                                                                  Comparator<SceneNode<?>> comparator) {
        return findBestInRectWithExpand(scene, leftBottom.x(), leftBottom.y(),
                leftBottom.x() + length, leftBottom.y() + width, maxExpandRounds, filter, comparator);
    }

    /** 范围扩大取最优（中心点 + 正方形半边长） */
    public static Optional<SceneNode<?>> findBestInRectWithExpand(Scene scene, Position center, int halfSide,
                                                                  int maxExpandRounds,
                                                                  Predicate<SceneNode<?>> filter,
                                                                  Comparator<SceneNode<?>> comparator) {
        return findBestInRectWithExpand(scene, center.x() - halfSide, center.y() - halfSide,
                center.x() + halfSide, center.y() + halfSide, maxExpandRounds, filter, comparator);
    }

    /**
     * 在矩形范围内取距离某点最近的一个节点，若无结果则逐轮扩大范围
     *
     * @param scene           场景
     * @param center          中心点
     * @param halfWidth       矩形半宽
     * @param halfHeight      矩形半高
     * @param maxExpandRounds 最多扩大轮数（0 表示不扩大）
     * @param filter          条件过滤，为 null 表示不过滤
     * @param positionGetter  从节点得到代表坐标，若返回 null 则跳过该节点
     * @return 距离 center 最近的节点，若无则 empty
     */
    public static Optional<SceneNode<?>> findNearestInRectWithExpand(Scene scene, Position center,
                                                                     int halfWidth, int halfHeight,
                                                                     int maxExpandRounds,
                                                                     Predicate<SceneNode<?>> filter,
                                                                     Function<SceneNode<?>, Position> positionGetter) {
        Map<Long, SceneNode<?>> acc = expandCollect(scene,
                center.x() - halfWidth, center.y() - halfHeight,
                center.x() + halfWidth, center.y() + halfHeight,
                maxExpandRounds, null, filter, m -> !m.isEmpty());
        return pickNearest(new ArrayList<>(acc.values()), center, positionGetter);
    }

    /** 范围扩大取最近（中心点 + 正方形半边长） */
    public static Optional<SceneNode<?>> findNearestInRectWithExpand(Scene scene, Position center, int halfSide,
                                                                      int maxExpandRounds,
                                                                      Predicate<SceneNode<?>> filter,
                                                                      Function<SceneNode<?>, Position> positionGetter) {
        return findNearestInRectWithExpand(scene, center, halfSide, halfSide, maxExpandRounds, filter, positionGetter);
    }

    // ==================== 内部实现 ====================

    /**
     * 统一的扩大收集逻辑
     * <p>从初始矩形开始，将范围转化为 AoiGrid 区域并收集节点；每轮向四周扩大一个 DETAIL 网格步长。</p>
     * <p>已检查过的 AoiGrid 会记录并跳过，仅对新格子收集节点并合并到已有结果，避免重复遍历。</p>
     *
     * @param scene           场景
     * @param x1              初始矩形左下角 X
     * @param y1              初始矩形左下角 Y
     * @param x2              初始矩形右上角 X
     * @param y2              初始矩形右上角 Y
     * @param maxExpandRounds 最多扩大轮数
     * @param nodeType        节点类型过滤，为 null 不限
     * @param filter          条件过滤，为 null 不限
     * @param stopCondition   每轮收集后的终止判断，返回 true 则提前结束
     * @return 收集到的节点 Map（按 id 去重）
     */
    private static Map<Long, SceneNode<?>> expandCollect(Scene scene, int x1, int y1, int x2, int y2,
                                                         int maxExpandRounds,
                                                         Class<?> nodeType,
                                                         Predicate<SceneNode<?>> filter,
                                                         Predicate<Map<Long, SceneNode<?>>> stopCondition) {
        Map<Long, SceneNode<?>> acc = new LinkedHashMap<>();
        Set<AoiGrid> seenGrids = new HashSet<>();
        int curX1 = x1, curY1 = y1, curX2 = x2, curY2 = y2;
        GridContainer container = scene.getMultiGridContainer().getGridContainer(DEFAULT_LAYER);
        for (int round = 0; round <= maxExpandRounds; round++) {
            int length = Math.max(0, curX2 - curX1);
            int width = Math.max(0, curY2 - curY1);
            List<AoiGrid> grids = container.getGridsInRect(Position.valueOf(scene, curX1, curY1), length, width);
            for (AoiGrid grid : grids) {
                if (seenGrids.contains(grid)) {
                    continue;
                }
                seenGrids.add(grid);
                collectNodesFromGrid(grid, nodeType, filter, acc);
            }
            if (stopCondition.test(acc)) {
                break;
            }
            curX1 -= EXPAND_STEP;
            curY1 -= EXPAND_STEP;
            curX2 += EXPAND_STEP;
            curY2 += EXPAND_STEP;
        }
        return acc;
    }

    /**
     * 将矩形范围内满足条件的节点收集到 Map 中
     * <p>范围先转化为 AoiGrid 组成的区域（可能略大于传入范围），仅从这些格子中取节点，不再做 inRange 精确校验；精确距离可由 filter 控制。</p>
     * <p>已在 Map 中的节点通过 containsKey O(1) 跳过。</p>
     */
    private static void collectInto(Scene scene, int x1, int y1, int x2, int y2,
                                    Class<?> nodeType, Predicate<SceneNode<?>> filter,
                                    Map<Long, SceneNode<?>> into) {
        GridContainer container = scene.getMultiGridContainer().getGridContainer(DEFAULT_LAYER);
        int length = Math.max(0, x2 - x1);
        int width = Math.max(0, y2 - y1);
        List<AoiGrid> grids = container.getGridsInRect(Position.valueOf(scene, x1, y1), length, width);
        for (AoiGrid grid : grids) {
            collectNodesFromGrid(grid, nodeType, filter, into);
        }
    }

    /**
     * 从单个 AoiGrid 中收集满足类型与条件的节点到 Map
     */
    private static void collectNodesFromGrid(AoiGrid grid, Class<?> nodeType, Predicate<SceneNode<?>> filter,
                                             Map<Long, SceneNode<?>> into) {
        for (SceneNode<?> node : grid.getNodes().values()) {
            if (into.containsKey(node.getId())) {
                continue;
            }
            if (nodeType != null && !nodeType.isInstance(node)) {
                continue;
            }
            if (filter != null && !filter.test(node)) {
                continue;
            }
            into.put(node.getId(), node);
        }
    }

    /**
     * 将类型 + 条件合并为统一的 Predicate
     */
    @SuppressWarnings("unchecked")
    private static <N extends SceneNode<?>> Predicate<SceneNode<?>> wrapFilter(Class<N> nodeType, Predicate<N> filter) {
        if (filter == null) {
            return null;
        }
        return node -> nodeType.isInstance(node) && filter.test((N) node);
    }

    /**
     * 从候选列表中挑出距离 center 最近的一个
     */
    private static Optional<SceneNode<?>> pickNearest(List<SceneNode<?>> list, Position center,
                                                      Function<SceneNode<?>, Position> positionGetter) {
        if (list.isEmpty()) {
            return Optional.empty();
        }
        int cx = center.x(), cy = center.y();
        SceneNode<?> best = null;
        long bestDistSq = Long.MAX_VALUE;
        for (SceneNode<?> node : list) {
            Position pos = positionGetter.apply(node);
            if (pos == null) {
                continue;
            }
            long dx = (long) pos.x() - cx;
            long dy = (long) pos.y() - cy;
            long distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = node;
            }
        }
        return Optional.ofNullable(best);
    }
}
