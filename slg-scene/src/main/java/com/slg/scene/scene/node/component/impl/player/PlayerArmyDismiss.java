package com.slg.scene.scene.node.component.impl.player;

import com.slg.scene.scene.node.component.impl.common.DestroyComponent;
import com.slg.scene.scene.node.node.model.impl.PlayerArmy;

/**
 * 玩家军队解散组件
 * <p>军队回城时由交互方调用，执行解散逻辑（如从场景移除、归还兵力等）。</p>
 *
 * @author yangxunan
 * @date 2026/2/5
 */
public class PlayerArmyDismiss extends DestroyComponent<PlayerArmy> {

    public PlayerArmyDismiss(PlayerArmy belongNode) {
        super(belongNode);
    }

    @Override
    public void onDestroy() {
        // 具体逻辑后续实现（从场景 despawn、归还兵力等）
    }
}
