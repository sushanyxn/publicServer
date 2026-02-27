package com.slg.scene.scene.base.model;

import com.slg.scene.scene.aoi.model.MapData;
import com.slg.scene.scene.node.node.model.StaticNode;

/**
 * 阻挡容器
 * <p>维护场景格子的阻挡状态（地形 + 动态节点），与 {@link BlockType} 配合使用。</p>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public class BlockContainer {

    private final byte[] blocks;

    /** 地图宽度（格子数，有效 x 为 0..mapWidth-1） */
    private final int mapWidth;

    /** 地图高度（格子数，有效 y 为 0..mapHeight-1） */
    private final int mapHeight;

    public BlockContainer(Scene scene) {
        MapData mapData = scene.getMapData();
        this.mapWidth = mapData.getWidth();
        this.mapHeight = mapData.getHeight();
        int size = this.mapWidth * this.mapHeight;
        byte[] mapBlocks = mapData.getBlocks();
        if (mapBlocks != null && mapBlocks.length == size) {
            this.blocks = mapBlocks.clone();
        } else {
            this.blocks = new byte[size];
        }
    }

    /**
     * 指定格子是否存在任意类型的阻挡
     */
    public boolean isBlock(Position position) {
        return isBlock(position.x(), position.y());
    }

    /**
     * 指定坐标是否存在任意类型的阻挡
     */
    public boolean isBlock(int x, int y) {
        if (!inBounds(x, y)) {
            return true;
        }
        return (blocks[index(x, y)] & BlockType.BLOCK) != 0;
    }

    /**
     * 指定矩形区域内是否全部无阻挡
     * <p>矩形为左下角 (x0,y0)，向右延伸 length、向上延伸 width。</p>
     *
     * @param x0     左下角 X
     * @param y0     左下角 Y
     * @param length X 方向格子数
     * @param width  Y 方向格子数
     * @return true 表示该矩形内全部无阻挡，false 表示存在阻挡或越界
     */
    public boolean isRectNonBlocking(int x0, int y0, int length, int width) {
        for (int dy = 0; dy < width; dy++) {
            for (int dx = 0; dx < length; dx++) {
                if (isBlock(x0 + dx, y0 + dy)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 该静态节点占用的矩形内是否全部无阻挡（可在此落点出生）
     */
    public boolean canSpawn(StaticNode<?> node) {
        int x0 = node.getPosition().x();
        int y0 = node.getPosition().y();
        int len = node.getLength();
        int wid = node.getWidth();
        for (int dy = 0; dy < wid; dy++) {
            for (int dx = 0; dx < len; dx++) {
                if (isBlock(x0 + dx, y0 + dy)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 将静态节点占用的矩形标记为动态阻挡
     */
    public void addBlock(StaticNode<?> node) {
        setBlockInRect(node, BlockType.DYNAMIC_BLOCK, true);
    }

    /**
     * 清除静态节点占用矩形的动态阻挡
     */
    public void removeBlock(StaticNode<?> node) {
        setBlockInRect(node, BlockType.DYNAMIC_BLOCK, false);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < mapWidth && y >= 0 && y < mapHeight;
    }

    private int index(int x, int y) {
        return y * mapWidth + x;
    }

    private void setBlockInRect(StaticNode<?> node, byte mask, boolean add) {
        int x0 = node.getPosition().x();
        int y0 = node.getPosition().y();
        int len = node.getLength();
        int wid = node.getWidth();
        for (int dy = 0; dy < wid; dy++) {
            for (int dx = 0; dx < len; dx++) {
                int x = x0 + dx;
                int y = y0 + dy;
                if (inBounds(x, y)) {
                    int idx = index(x, y);
                    if (add) {
                        blocks[idx] |= mask;
                    } else {
                        blocks[idx] &= ~mask;
                    }
                }
            }
        }
    }
}
