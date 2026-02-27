package com.slg.scene.scene.node.node.model;

import com.slg.common.util.MathUtil;
import com.slg.scene.scene.aoi.model.AoiGrid;
import com.slg.scene.scene.aoi.model.GridContainer;
import com.slg.scene.scene.base.model.FPosition;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.node.owner.NodeOwner;
import lombok.Getter;

/**
 * 静态节点抽象基类
 * <p>表示场景中位置与范围固定的节点（如建筑、资源点等），由左下角点、长、宽确定矩形占位。</p>
 * <p>与 {@link RouteNode} 对称：RouteNode 为线段（起点-终点），StaticNode 为矩形（左下角+长宽）。</p>
 *
 * @param <T> 节点拥有者类型
 * @author yangxunan
 * @date 2026/2/4
 */
@Getter
public abstract class StaticNode<T extends NodeOwner> extends SceneNode<T> {

    /**
     * 矩形左下角坐标
     */
    protected Position position;

    /**
     * 矩形长度（X 方向，向右延伸）
     */
    protected int length;

    /**
     * 矩形宽度（Y 方向，向上延伸）
     */
    protected int width;

    /**
     * 矩形中心点（初始化时计算并缓存，支持小数）
     */
    protected FPosition centerPosition;

    public StaticNode(T owner, long id, Position position){
        super(owner, id);
        this.position = position;
        this.length = 1;
        this.width = 1;
        this.centerPosition = FPosition.valueOf(
                position.sceneId(),
                position.x() * FPosition.SCALE + 50,
                position.y() * FPosition.SCALE + 50
        );
    }

    public StaticNode(T owner, long id, Position position, int length, int width){
        super(owner, id);
        this.position = position;
        this.length = length;
        this.width = width;
        this.centerPosition = FPosition.valueOf(
                position.sceneId(),
                position.x() * FPosition.SCALE + length * 50,
                position.y() * FPosition.SCALE + width * 50
        );
    }

    @Override
    public void enterGrid(GridContainer gridContainer){
        if (length == 1 && width == 1) {
            AoiGrid grid = gridContainer.getGrid(position.x(), position.y());
            if (grid != null) {
                grid.addSceneNode(this);
            }
        } else{
            gridContainer.foreachRectGrid(position, length, width, grid -> grid.addSceneNode(this));
        }
    }

    @Override
    public void exitGrid(GridContainer gridContainer){
        if (length == 1 && width == 1) {
            AoiGrid grid = gridContainer.getGrid(position.x(), position.y());
            if (grid != null) {
                grid.removeSceneNode(this);
            }
        } else{
            gridContainer.foreachRectGrid(position, length, width, grid -> grid.removeSceneNode(this));
        }
    }

    @Override
    public boolean inRange(int x1, int y1, int x2, int y2){
        if (length == 1 && width == 1) {
            return MathUtil.pointInRect(position.x(), position.y(), x1, y1, x2, y2);
        }
        int myX1 = position.x();
        int myY1 = position.y();
        int myX2 = position.x() + length;
        int myY2 = position.y() + width;
        return MathUtil.intervalOverlap(myX1, myX2, x1, x2) && MathUtil.intervalOverlap(myY1, myY2, y1, y2);
    }

    protected final void setCenterPosition(FPosition centerPosition){
        throw new IllegalStateException("Cannot set center position");
    }

    public final void setPosition(Position position){
        if (spawned) {
            throw new IllegalStateException("Cannot set position when node is already spawned");
        }
        this.position = position;
        this.centerPosition = FPosition.valueOf(
                position.sceneId(),
                position.x() * FPosition.SCALE + length * 50,
                position.y() * FPosition.SCALE + width * 50
        );
    }
}
