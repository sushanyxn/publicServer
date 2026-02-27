package com.slg.scene.scene.node.node.model.impl;

import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.node.component.impl.army.MultiArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.assemble.AssembleArmyReportComponent;
import com.slg.scene.scene.node.component.impl.assemble.AssembleComponent;
import com.slg.scene.scene.node.component.impl.assemble.AssembleDismissComponent;
import com.slg.scene.scene.node.component.impl.assemble.AssembleIdleComponent;
import com.slg.scene.scene.node.component.impl.common.SelectTargetComponent;
import com.slg.scene.scene.node.node.model.RouteNode;

/**
 * 集结军队节点（行军线）
 * 由多名玩家的军队集结而成，拥有者为集结发起人（队长）。
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public class AssembleArmy extends RouteNode<ScenePlayer> {

    public AssembleArmy(ScenePlayer leader, long id) {
        super(leader, id);
    }

    @Override
    public void initComponents() {

        // 行军组件 管理移动，到达
        registerComponent(new SelectTargetComponent(this));
        // 复合军队详情组件 管理成员军队 提供战斗数据
        registerComponent(new MultiArmyDetailComponent(this, getOwner().getId()));
        // 战报组件 声明本节点参与战斗时需要的战报模块
        registerComponent(new AssembleArmyReportComponent(this));
        // 集结组件 管理集结等待，集结出发，成员加入等逻辑
        registerComponent(new AssembleComponent(this));
        // 集结发呆组件 处理目标无效，目标是空地等情况
        registerComponent(new AssembleIdleComponent(this));
        // 集结销毁组件，解散集结，分裂成玩家军队
        registerComponent(new AssembleDismissComponent(this));
    }

    @Override
    public GridLayer belongHighLayer() {
        return GridLayer.LAYER3;
    }
}
