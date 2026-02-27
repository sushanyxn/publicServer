package com.slg.game.bean.condition;

import com.slg.common.progress.bean.IProgressCondition;
import com.slg.game.base.player.model.Player;
import com.slg.game.bean.event.IPlayerProgressEvent;

/**
 * 需要支持进度更新的条件
 * 需要关联监听事件
 *
 * @author yangxunan
 * @date 2026/1/29
 */
public interface IPlayerProgressCondition<E extends IPlayerProgressEvent> extends IPlayerCondition, IProgressCondition<Player, E> {
}
