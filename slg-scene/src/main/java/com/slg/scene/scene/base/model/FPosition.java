package com.slg.scene.scene.base.model;

import com.slg.net.message.clientmessage.scene.packet.FPositionVO;
import com.slg.net.message.clientmessage.scene.packet.PositionVO;
import com.slg.scene.scene.base.manager.SceneManager;

/**
 * 亚格子精度坐标（不可变，定点数放大 100 倍）
 * <p>使用 int 存储，实际坐标 = 存储值 / 100，精度 0.01。使用时只允许整体替换，不允许修改内部参数。</p>
 * <p>构造时预计算整数格坐标 gridX、gridY（= x/SCALE、y/SCALE），避免各处重复除 SCALE 运算。</p>
 * <p>坐标系与 {@link Position} 一致：原点在地图左下角，X 向右、Y 向上。</p>
 *
 * @param sceneId 所属场景ID
 * @param x       X 坐标（放大 100 倍，即百分之一格为单位）
 * @param y       Y 坐标（放大 100 倍）
 * @param gridX   整数格 X（= x / SCALE），构造时计算并保存
 * @param gridY   整数格 Y（= y / SCALE），构造时计算并保存
 * @author yangxunan
 * @date 2026/2/4
 */
public record FPosition(long sceneId, int x, int y, int gridX, int gridY) {

    /** 放大倍数，实际值 = 存储值 / SCALE */
    public static final int SCALE = 100;

    /**
     * 紧凑构造：根据 x、y 统一计算并写入 gridX、gridY，保证与 SCALE 一致。
     */
    public FPosition {
        gridX = x / SCALE;
        gridY = y / SCALE;
    }

    /**
     * 通过场景ID创建亚格子坐标（整数格坐标在构造时自动计算）
     *
     * @param sceneId 场景ID
     * @param x       缩放后 X（如 FPosition 内部刻度）
     * @param y       缩放后 Y
     * @return 新实例
     */
    public static FPosition valueOf(long sceneId, int x, int y) {
        return new FPosition(sceneId, x, y, 0, 0);
    }

    /**
     * 通过场景实体创建亚格子坐标（整数格坐标在构造时自动计算）
     *
     * @param scene 场景实体
     * @param x     缩放后 X（如 FPosition 内部刻度）
     * @param y     缩放后 Y
     * @return 新实例
     */
    public static FPosition valueOf(Scene scene, int x, int y) {
        return new FPosition(scene.getSceneId(), x, y, 0, 0);
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
     * 转换为整数格子坐标 VO（不足 1 的部分省略，即截断；不包含 sceneId）
     */
    public PositionVO toVO() {
        PositionVO vo = new PositionVO();
        vo.setX(gridX);
        vo.setY(gridY);
        return vo;
    }

    /**
     * 转换为亚格子坐标 VO（保持 100 倍刻度，供客户端还原；不包含 sceneId）
     */
    public FPositionVO toFPositionVO() {
        FPositionVO vo = new FPositionVO();
        vo.setX(x);
        vo.setY(y);
        return vo;
    }

    /**
     * 转化为整数格子坐标（携带 sceneId）
     *
     * @return 整数格子坐标
     */
    public Position toPosition() {
        return Position.valueOf(sceneId, gridX, gridY);
    }
}
