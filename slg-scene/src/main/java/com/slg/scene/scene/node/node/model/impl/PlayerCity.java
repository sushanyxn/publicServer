package com.slg.scene.scene.node.node.model.impl;

import com.slg.net.message.clientmessage.scene.packet.PlayerCityVO;
import com.slg.net.message.clientmessage.scene.packet.PlayerOwnerVO;
import com.slg.net.message.clientmessage.scene.packet.SceneNodeVO;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.scene.aoi.model.GridLayer;
import com.slg.scene.scene.base.model.Position;
import com.slg.scene.scene.node.component.impl.common.BlockComponent;
import com.slg.scene.scene.node.component.impl.common.FightComponent;
import com.slg.scene.scene.node.component.impl.common.GarrisonComponent;
import com.slg.scene.scene.node.component.impl.player.PlayerCityInteractiveComponent;
import com.slg.scene.scene.node.component.impl.player.PlayerCityReportComponent;
import com.slg.scene.scene.node.node.model.StaticNode;

/**
 * 玩家主城节点
 * <p>表示场景中玩家主城，为矩形静态节点，默认边长 3*3。</p>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public class PlayerCity extends StaticNode<ScenePlayer> {

    /** 默认主城边长（长=宽=3） */
    public static final int DEFAULT_SIZE = 3;

    /**
     * 使用默认尺寸创建玩家主城
     *
     * @param owner    拥有者（场景玩家）
     * @param id       节点唯一 ID
     * @param position 主城左下角坐标
     */
    public PlayerCity(ScenePlayer owner, long id, Position position) {
        super(owner, id, position, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    @Override
    public void initComponents() {
        // 阻挡组件 处理场景阻挡
        registerComponent(new BlockComponent<>(this));
        // 驻守组件 管理建筑驻军 提供战斗数据
        registerComponent(new GarrisonComponent(this));
        // 战斗组件 与来访军队战斗
        registerComponent(new FightComponent(this));
        // 战报组件 声明本节点参与战斗时需要的战报模块
        registerComponent(new PlayerCityReportComponent(this));
        // 主城交互组件 提供参与集结/回城/驻守/攻打等交互动作的判定
        registerComponent(new PlayerCityInteractiveComponent(this));
    }

    @Override
    public SceneNodeVO toVO() {
        PlayerCityVO vo = new PlayerCityVO();
        vo.setId(getId());
        PlayerOwnerVO ownerVO = getOwner().toOwnerVO();
        ownerVO.setCityPosition(getPosition().toVO());
        vo.setOwner(ownerVO);
        vo.setPosition(getPosition().toVO());
        return vo;
    }

    @Override
    public GridLayer belongHighLayer() {
        return GridLayer.LAYER3;
    }

}
