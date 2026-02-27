package com.slg.scene.scene.base.model;

import com.slg.net.message.clientmessage.scene.packet.PositionVO;
import com.slg.scene.scene.base.manager.SceneManager;

/**
 * 世界坐标（不可变）
 * <p>表示场景中的二维坐标位置，使用时只允许整体替换，不允许修改内部参数。</p>
 *
 * <p><b>坐标系：</b></p>
 * <ul>
 *   <li>整数坐标，以格子为单位</li>
 *   <li>原点(0,0)在地图左下角</li>
 *   <li>X轴向右递增，Y轴向上递增</li>
 * </ul>
 *
 * @param sceneId 所属场景ID
 * @param x X坐标
 * @param y Y坐标
 * @author yangxunan
 * @date 2026/2/3
 */
public record Position(long sceneId, int x, int y) {

    /**
     * 通过场景ID创建坐标
     *
     * @param sceneId 场景ID
     * @param x       X坐标
     * @param y       Y坐标
     * @return 坐标实例
     */
    public static Position valueOf(long sceneId, int x, int y) {
        return new Position(sceneId, x, y);
    }

    /**
     * 通过场景实体创建坐标
     *
     * @param scene 场景实体
     * @param x     X坐标
     * @param y     Y坐标
     * @return 坐标实例
     */
    public static Position valueOf(Scene scene, int x, int y) {
        return new Position(scene.getSceneId(), x, y);
    }

    /**
     * 获取所属场景实体
     *
     * @return 场景实体
     */
    public Scene getScene() {
        return SceneManager.getInstance().getScene(sceneId);
    }

    /**
     * 转换为协议 VO（不包含 sceneId）
     */
    public PositionVO toVO() {
        PositionVO positionVO = new PositionVO();
        positionVO.setX(x);
        positionVO.setY(y);
        return positionVO;
    }
}
