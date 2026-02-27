package com.slg.scene.scene.node.node.model.impl;

import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.node.component.impl.army.PlayerArmyDetailComponent;
import com.slg.scene.scene.node.component.impl.army.PlayerArmyReportComponent;
import com.slg.scene.scene.node.component.impl.player.PlayerArmyDismiss;
import com.slg.scene.scene.node.component.impl.player.PlayerArmyIdle;
import com.slg.scene.scene.node.component.impl.common.SelectTargetComponent;
import com.slg.scene.scene.node.node.model.RouteNode;

/**
 * 玩家军队节点（行军线）
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public class PlayerArmy extends RouteNode<ScenePlayer> {

    public PlayerArmy(ScenePlayer owner, long id) {
        super(owner, id);
    }

    @Override
    public void initComponents() {

        // 行军组件 管理移动，到达
        registerComponent(new SelectTargetComponent(this));
        // 军队详情组件 展示军队组成 提供战斗数据
        registerComponent(new PlayerArmyDetailComponent(this));
        // 战报组件 声明本节点参与战斗时需要的战报模块
        registerComponent(new PlayerArmyReportComponent(this));
        // 军队发呆组件 处理目标是空地 目标无效时的表现（原地扎营/回城）
        registerComponent(new PlayerArmyIdle(this));
        // 军队销毁组件 回城时触发，返还兵力和英雄到内城
        registerComponent(new PlayerArmyDismiss(this));
    }

    @Override
    public GridLayer belongHighLayer(){
        return GridLayer.LAYER3;
    }
}
