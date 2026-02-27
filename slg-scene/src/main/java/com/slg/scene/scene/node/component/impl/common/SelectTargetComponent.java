package com.slg.scene.scene.node.component.impl.common;

import com.slg.common.executor.Executor;
import com.slg.common.util.MathUtil;
import com.slg.scene.scene.node.component.AbstractNodeComponent;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.base.model.FPosition;
import com.slg.scene.scene.node.component.impl.army.IdleComponent;
import com.slg.scene.scene.node.model.ArmyActionType;
import com.slg.scene.scene.node.node.model.RouteNode;
import com.slg.scene.scene.node.node.model.StaticNode;
import lombok.Getter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 选择目标组件 (行军组件，移动组件)
 * 表示此 node 具有选择目标并朝目标移动的能力。仅支持静态 node 作为目标，目的地使用目标中心坐标；空地时目的地由 RouteNode.endPos 表示。
 * 选择目标时需传入行军目的（{@link ArmyActionType}），到达时一并传入交互方法。
 *
 * @author yangxunan
 * @date 2026/2/4
 */
@Getter
public class SelectTargetComponent extends AbstractNodeComponent<RouteNode<?>> {

    /**
     * 开始移动时间（毫秒）
     * 中途使用加速道具后，会更新为使用加速时的时刻
     */
    private long startTime;

    /**
     * 当前段的起点（亚格子精度，不一定是行军线构造时的起点）
     * 中途使用加速道具后，起点变为使用加速时的坐标
     */
    private FPosition startPos;

    /**
     * 到达目标时间（毫秒）
     */
    private long endTime;

    /**
     * 目标静态 node，为空时表示目的地为空地
     */
    private StaticNode<?> targetNode;

    /**
     * 行军目的，选择目标时传入并保存，到达时传入交互方法
     */
    private ArmyActionType armyActionType = ArmyActionType.DEFAULT;

    /**
     * 到达目标的定时任务，用于在 endTime 触发并投递到业务线程
     */
    private ScheduledFuture<?> arrivedSchedule;

    /**
     * 是否已到达；到达时置为 true，start 时重置为 false。已到达时不允许加速。
     */
    private boolean arrived;

    public SelectTargetComponent(RouteNode<?> belongNode){
        super(belongNode);
    }

    @Override
    public ComponentEnum getComponentEnum(){
        return ComponentEnum.SelectTarget;
    }

    /**
     * 只能在场景线程调用，选择静态 node 作为目标。会设置自身与 RouteNode 的起始坐标为当前坐标，终点为目标中心坐标。
     * 若已出生在场景中则不允许重新 select，须先从场景移除。
     *
     * @param targetNode 目标静态节点
     * @param purpose    行军目的，到达时传入交互方法
     */
    public void selectTarget(StaticNode<?> targetNode, ArmyActionType purpose){
        if (belongNode.isSpawned()) {
            return;
        }
        this.targetNode = targetNode;
        this.armyActionType = purpose != null ? purpose : ArmyActionType.DEFAULT;
        FPosition cur = getCurrentPosition();
        if (cur != null) {
            belongNode.setStartPos(cur);
            this.startPos = cur;
        }
        if (targetNode != null && targetNode.getCenterPosition() != null) {
            belongNode.setEndPos(targetNode.getCenterPosition());
        }
    }

    /**
     * 只能在场景线程调用，选择空地。会设置自身与 RouteNode 的起始坐标为当前坐标，终点为 targetPos。
     * 若已出生在场景中则不允许重新 select，须先从场景移除。
     *
     * @param targetPos 目标坐标（空地）
     */
    public void selectTarget(FPosition targetPos){
        if (belongNode.isSpawned()) {
            return;
        }
        this.targetNode = null;
        this.armyActionType = ArmyActionType.DEFAULT;
        FPosition cur = getCurrentPosition();
        if (cur != null) {
            belongNode.setStartPos(cur);
            this.startPos = cur;
        }
        if (targetPos != null) {
            belongNode.setEndPos(targetPos);
        }
    }

    /**
     * 只能在场景线程调用，开始移动。
     * 使用行军线起点、终点与速度计算 endTime，在场景线程创建定时器，到点后投递 arrived 到业务线程。
     */
    public void start(){
        if (belongNode.getEndPos() == null) {
            return;
        }
        cancelArrivedSchedule();
        arrived = false;
        startPos = belongNode.getStartPos();
        startTime = System.currentTimeMillis();
        int speed = getSpeed();
        FPosition endPos = belongNode.getEndPos();
        double dist = (startPos == null || endPos == null) ? 0 : MathUtil.distanceInGrid(startPos.x(), startPos.y(), endPos.x(), endPos.y(), FPosition.SCALE);
        long totalMs = (long) (dist * 1000 * 100 / Math.max(1, speed));
        endTime = startTime + totalMs;

        long delay = Math.max(0, endTime - System.currentTimeMillis());
        long arriveKey = getArrivedDispatchKey();
        arrivedSchedule = Executor.Scene.schedule(() -> {
            cancelArrivedSchedule();
            arrived = true;
            Executor.SceneNode.execute(arriveKey, this::arrived);
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 移动加速：在当前速度基础上的加速万分比，通过「时间 = 距离/速度」的倒数关系转换为剩余时间的比例。
     * 新剩余时间 = 当前剩余时间 × 10000 / (10000 + speedRate)。多次加速时每次都在当前剩余时间上生效。
     *
     * @param speedRate 对当前速度的加速万分比，如 5000 表示速度变为 1.5 倍、剩余时间变为 2/3，10000 表示 2 倍、剩余时间变为 1/2
     */
    public void speed(int speedRate){
        if (arrived || belongNode.getEndPos() == null || startPos == null) {
            return;
        }
        cancelArrivedSchedule();
        FPosition cur = getCurrentPosition();
        if (cur != null) {
            startPos = cur;
        }
        long now = System.currentTimeMillis();
        startTime = now;
        long remainingMs = Math.max(0, endTime - now);
        if (speedRate > 0) {
            remainingMs = remainingMs * 10000 / (10000 + speedRate);
        }
        endTime = startTime + remainingMs;

        long delay = Math.max(0, endTime - System.currentTimeMillis());
        long arriveKey = getArrivedDispatchKey();
        arrivedSchedule = Executor.Scene.schedule(() -> {
            cancelArrivedSchedule();
            arrived = true;
            Executor.SceneNode.execute(arriveKey, this::arrived);
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 到达目标后的逻辑（在业务线程执行）。
     * 仅做目标有效性判断；若行军方带交互组件且目标为静态 node，则走交互组件流程。
     */
    public void arrived(){

        InteractiveComponent<?> interactable;
        if (targetNode == null || !targetNode.isSpawned() || belongNode.getEndPos() != targetNode.getCenterPosition()) {
            // 无目标交互 或 交互目标无效
            interactable = null;
        } else {
            interactable = targetNode.getComponent(ComponentEnum.Interactive);
        }
        if (interactable != null) {
            // 如果目标 node 具有交互能力，由对方的交互组件处理军队的后续行为（如驻防、战斗、进入战斗队列、回城等），并传入行军目的
            interactable.onInteractedBy(belongNode, armyActionType);
        } else {
            // 目标 node 没有交互能力，使用军队的无目标组件（发呆组件）
            IdleComponent idle = belongNode.getComponent(ComponentEnum.Idle);
            if (idle != null) {
                idle.onArrivedWithNoTarget();
            }
        }
    }

    /**
     * 计算并返回当前坐标（亚格子精度，根据 startPos、终点、startTime、endTime 插值）
     */
    public FPosition getCurrentPosition(){
        if (startPos == null) {
            return belongNode.getStartPos();
        }
        FPosition end = belongNode.getEndPos();
        if (end == null) {
            return startPos;
        }
        long now = System.currentTimeMillis();
        if (now >= endTime) {
            return end;
        }
        if (now <= startTime) {
            return startPos;
        }
        double progress = (double) (now - startTime) / (endTime - startTime);
        int x = (int) (startPos.x() + (end.x() - startPos.x()) * progress);
        int y = (int) (startPos.y() + (end.y() - startPos.y()) * progress);
        return FPosition.valueOf(startPos.sceneId(), x, y);
    }

    /**
     * 获取移动速度，后续接入功能模块（配置/科技等）。
     *
     * @return 每秒移动量，100 表示 1 格，放大 100 倍避免浮点
     */
    public int getSpeed(){
        return 100;
    }

    private void cancelArrivedSchedule(){
        if (arrivedSchedule != null) {
            arrivedSchedule.cancel(false);
            arrivedSchedule = null;
        }
    }

    /**
     * 投递 arrived 时使用的 key：目标 node 用其 id，空地用 RouteNode.endPos 格子坐标生成 key
     */
    private long getArrivedDispatchKey(){
        if (targetNode != null) {
            return targetNode.getId();
        }
        FPosition end = belongNode.getEndPos();
        if (end != null) {
            int gx = end.gridX();
            int gy = end.gridY();
            return ((long) gx << 32) | (gy & 0xFFFFFFFFL);
        }
        return belongNode.getId();
    }
}
