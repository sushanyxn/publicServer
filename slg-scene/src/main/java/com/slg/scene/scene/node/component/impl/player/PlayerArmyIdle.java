package com.slg.scene.scene.node.component.impl.player;

import com.slg.scene.scene.node.component.impl.army.IdleComponent;
import com.slg.scene.scene.node.node.model.impl.PlayerArmy;

/**
 * 玩家军队发呆组件
 * <p>挂载于 {@link PlayerArmy}，处理无目标到达、交互失败、回城等后续行为。</p>
 *
 * @author yangxunan
 * @date 2026-02-06
 */
public class PlayerArmyIdle extends IdleComponent<PlayerArmy> {

    public PlayerArmyIdle(PlayerArmy belongNode) {
        super(belongNode);
    }

    @Override
    public void onArrivedWithNoTarget() {
        // 玩家军队到达空地等无目标时的具体逻辑（如驻留、回城等）后续实现
    }

    @Override
    public void onInteractionFailed() {
        // 玩家军队交互失败时的具体逻辑（如提示、驻留等）后续实现
    }

    @Override
    public void onReturnToCity() {
        // 玩家军队回城流程，主城交互已通过 DestroyComponent 处理，本方法供其他场景使用
    }
}
