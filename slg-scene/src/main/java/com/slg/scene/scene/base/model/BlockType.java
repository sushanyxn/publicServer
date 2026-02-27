package com.slg.scene.scene.base.model;

/**
 * 阻挡类型（位标记，可组合）
 * <ul>
 *   <li>1000 0000 = 山水阻挡，不会被修改</li>
 *   <li>0100 0000 = 动态阻挡（由 StaticNode 占据，随节点出生/移除更新）</li>
 *   <li>其余位预留</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public interface BlockType {

    /** 山水/地形阻挡，只读 */
    byte SCENE_BLOCK = (byte) 0x80;

    /** 动态阻挡（StaticNode 占格） */
    byte DYNAMIC_BLOCK = (byte) 0x40;

    /** 任意阻挡（用于判断是否可通行/可落点） */
    byte BLOCK = (byte) (SCENE_BLOCK | DYNAMIC_BLOCK);

}
