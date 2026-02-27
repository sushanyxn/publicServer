package com.slg.scene.scene.node.node.model;

import com.slg.common.util.MathUtil;
import com.slg.net.message.clientmessage.army.packet.ArmyVO;
import com.slg.net.message.clientmessage.scene.packet.RouteNodeVO;
import com.slg.net.message.clientmessage.scene.packet.SceneNodeVO;
import com.slg.scene.scene.aoi.model.GridContainer;
import com.slg.scene.scene.aoi.model.Watcher;
import com.slg.scene.scene.base.model.FPosition;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.owner.NodeOwner;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行军线 node
 * 本质是正在移动的军队，使用亚格子精度坐标 FPosition。
 * 按照 场景 -> 行军线 -> 军队 的关系进行持有。
 *
 * @author yangxunan
 * @date 2026/2/4
 */
@Getter
public abstract class RouteNode<T extends NodeOwner> extends SceneNode<T> {

    /**
     * 线的起始位置（亚格子精度）
     */
    protected FPosition startPos;

    /**
     * 线的结束位置（亚格子精度）
     */
    protected FPosition endPos;

    /** 观察者列表（哪些玩家看见了此节点） 完全由tick控制 */
    protected Map<Long, Watcher> seeArmyList = new ConcurrentHashMap<>();

    public RouteNode(T owner, long id){
        super(owner, id);
    }

    @Override
    public SceneNodeVO toVO(){
        return RouteNodeVO.valueOf(startPos.toFPositionVO(), endPos.toFPositionVO(), toArmyVO());
    }

    /**
     * 由挂载的 {@link ArmyDetailComponent} 生成军队 VO，无该组件时返回 null。
     */
    public ArmyVO toArmyVO(){
        ArmyDetailComponent<?> comp = getComponent(ComponentEnum.ArmyDetail);
        return comp != null ? comp.toArmyVO() : null;
    }

    @Override
    public void enterGrid(GridContainer gridContainer){
        gridContainer.foreachLineGrid(startPos, endPos, grid -> grid.addSceneNode(this));
    }

    @Override
    public void exitGrid(GridContainer gridContainer){
        gridContainer.foreachLineGrid(startPos, endPos, grid -> grid.removeSceneNode(this));
    }

    @Override
    public boolean inRange(int x1, int y1, int x2, int y2){
        return MathUtil.segmentIntersectsRect(
                startPos.gridX(), startPos.gridY(),
                endPos.gridX(), endPos.gridY(),
                x1, y1, x2, y2);
    }

    public final void setStartPos(FPosition startPos){
        if (spawned) {
            throw new IllegalStateException("行军线在场景中时, 不允许修改坐标");
        }
        // 设置起点坐标
        this.startPos = startPos;
    }

    public final void setEndPos(FPosition endPos){
        if (spawned) {
            throw new IllegalStateException("行军线在场景中时, 不允许修改坐标");
        }
        // 设置终点坐标
        this.endPos = endPos;
    }
}
