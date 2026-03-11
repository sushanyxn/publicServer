package com.slg.game.base.login.event;

import com.slg.common.event.model.IEvent;
import com.slg.game.base.player.model.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 玩家登出事件
 * 在解绑会话、关闭连接之前，于玩家线程中抛出
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class PlayerLogoutEvent implements IEvent {

    private Player player;
}
