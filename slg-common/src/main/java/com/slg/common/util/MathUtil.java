package com.slg.common.util;

import java.util.function.BiConsumer;

/**
 * 数学计算工具类
 * 提供线段与矩形相交、线段经过网格枚举等几何算法，供场景 AOI、路径等业务复用。
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public final class MathUtil {

    private MathUtil() {
    }

    // ==================== 线段与矩形相交 ====================

    /**
     * 判断线段是否与矩形相交或包含于矩形内
     * <p>用于 AOI 视野判断：线段（如行军线）是否经过视野矩形。</p>
     * <p>矩形为左下角 (rectMinX, rectMinY)、右上角 (rectMaxX, rectMaxY)。</p>
     *
     * @param segStartX 线段起点 X
     * @param segStartY 线段起点 Y
     * @param segEndX   线段终点 X
     * @param segEndY   线段终点 Y
     * @param rectMinX  矩形左下角 X
     * @param rectMinY  矩形左下角 Y
     * @param rectMaxX  矩形右上角 X
     * @param rectMaxY  矩形右上角 Y
     * @return true 表示线段与矩形有交或线段在矩形内
     */
    public static boolean segmentIntersectsRect(
            int segStartX, int segStartY, int segEndX, int segEndY,
            int rectMinX, int rectMinY, int rectMaxX, int rectMaxY) {
        int segMinX = Math.min(segStartX, segEndX);
        int segMaxX = Math.max(segStartX, segEndX);
        int segMinY = Math.min(segStartY, segEndY);
        int segMaxY = Math.max(segStartY, segEndY);
        if (!intervalOverlap(segMinX, segMaxX, rectMinX, rectMaxX)
                || !intervalOverlap(segMinY, segMaxY, rectMinY, rectMaxY)) {
            return false;
        }
        if (pointInRect(segStartX, segStartY, rectMinX, rectMinY, rectMaxX, rectMaxY)
                || pointInRect(segEndX, segEndY, rectMinX, rectMinY, rectMaxX, rectMaxY)) {
            return true;
        }
        return segmentIntersectsHorizontal(segStartX, segStartY, segEndX, segEndY, rectMinY, rectMinX, rectMaxX)
                || segmentIntersectsHorizontal(segStartX, segStartY, segEndX, segEndY, rectMaxY, rectMinX, rectMaxX)
                || segmentIntersectsVertical(segStartX, segStartY, segEndX, segEndY, rectMinX, rectMinY, rectMaxY)
                || segmentIntersectsVertical(segStartX, segStartY, segEndX, segEndY, rectMaxX, rectMinY, rectMaxY);
    }

    /**
     * 点是否在矩形内（矩形为左下角 (x1,y1)、右上角 (x2,y2)）
     */
    public static boolean pointInRect(int px, int py, int x1, int y1, int x2, int y2) {
        return px >= x1 && px <= x2 && py >= y1 && py <= y2;
    }

    /**
     * 两区间 [a1,a2]、[b1,b2] 是否有重叠（端点可相等）
     */
    public static boolean intervalOverlap(int a1, int a2, int b1, int b2) {
        return Math.max(a1, b1) <= Math.min(a2, b2);
    }

    private static boolean segmentIntersectsHorizontal(int sx, int sy, int ex, int ey, int yLine, int x1, int x2) {
        int yMin = Math.min(sy, ey);
        int yMax = Math.max(sy, ey);
        if (yLine < yMin || yLine > yMax) {
            return false;
        }
        if (yMin == yMax) {
            return sy == yLine && intervalOverlap(Math.min(sx, ex), Math.max(sx, ex), x1, x2);
        }
        long dx = (long) (ex - sx) * (yLine - sy);
        int dy = ey - sy;
        int xCross = sx + (int) (dx / dy);
        return xCross >= x1 && xCross <= x2;
    }

    private static boolean segmentIntersectsVertical(int sx, int sy, int ex, int ey, int xLine, int y1, int y2) {
        int xMin = Math.min(sx, ex);
        int xMax = Math.max(sx, ex);
        if (xLine < xMin || xLine > xMax) {
            return false;
        }
        if (xMin == xMax) {
            return sx == xLine && intervalOverlap(Math.min(sy, ey), Math.max(sy, ey), y1, y2);
        }
        long dy = (long) (ey - sy) * (xLine - sx);
        int dx = ex - sx;
        int yCross = sy + (int) (dy / dx);
        return yCross >= y1 && yCross <= y2;
    }

    // ==================== 距离计算 ====================

    /**
     * 按缩放坐标计算两点间的欧氏距离（以「格」为单位）。
     * <p>常用于亚格子精度坐标（如 FPosition）：坐标先除以 scale 再求距离。</p>
     *
     * @param x1    起点 X（缩放后，如 FPosition.x()）
     * @param y1    起点 Y（缩放后）
     * @param x2    终点 X（缩放后）
     * @param y2    终点 Y（缩放后）
     * @param scale 坐标缩放倍数（如 FPosition.SCALE），必须 &gt; 0
     * @return 两点间距离（格），若 scale &lt;= 0 则返回 0
     */
    public static double distanceInGrid(int x1, int y1, int x2, int y2, int scale) {
        if (scale <= 0) {
            return 0;
        }
        double dx = (x2 - x1) / (double) scale;
        double dy = (y2 - y1) / (double) scale;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ==================== 线段经过的网格枚举（DDA） ====================

    /**
     * 枚举线段从 (x0,y0) 到 (x1,y1) 经过的所有网格，按网格步进，步数等于穿过的网格数。
     * <p>网格坐标与「世界坐标 / gridLength」一致，即格 (gx, gy) 对应世界范围 [gx*L,(gx+1)*L) × [gy*L,(gy+1)*L)。</p>
     *
     * @param x0         起点 X（世界坐标）
     * @param y0         起点 Y（世界坐标）
     * @param x1         终点 X（世界坐标）
     * @param y1         终点 Y（世界坐标）
     * @param gridLength 网格边长
     * @param action     对每个经过的网格坐标 (gx, gy) 回调一次
     */
    public static void forEachGridOnSegment(
            int x0, int y0, int x1, int y1, int gridLength,
            BiConsumer<Integer, Integer> action) {
        int L = gridLength;
        int gx = x0 / L;
        int gy = y0 / L;
        int gxEnd = x1 / L;
        int gyEnd = y1 / L;

        action.accept(gx, gy);
        if (gx == gxEnd && gy == gyEnd) {
            return;
        }

        int dx = x1 - x0;
        int dy = y1 - y0;
        int stepX = Integer.compare(dx, 0);
        int stepY = Integer.compare(dy, 0);

        double tDeltaX = dx != 0 ? (double) L / Math.abs(dx) : Double.POSITIVE_INFINITY;
        double tDeltaY = dy != 0 ? (double) L / Math.abs(dy) : Double.POSITIVE_INFINITY;
        double tMaxX = dx > 0 ? ((gx + 1) * L - x0) / (double) dx
                : dx < 0 ? (gx * L - x0) / (double) dx
                : Double.POSITIVE_INFINITY;
        double tMaxY = dy > 0 ? ((gy + 1) * L - y0) / (double) dy
                : dy < 0 ? (gy * L - y0) / (double) dy
                : Double.POSITIVE_INFINITY;

        while (true) {
            if (tMaxX <= tMaxY) {
                gx += stepX;
                if (gx == gxEnd && gy == gyEnd) {
                    action.accept(gx, gy);
                    return;
                }
                tMaxX += tDeltaX;
            } else {
                gy += stepY;
                if (gx == gxEnd && gy == gyEnd) {
                    action.accept(gx, gy);
                    return;
                }
                tMaxY += tDeltaY;
            }
            action.accept(gx, gy);
        }
    }
}
