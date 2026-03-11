package com.slg.game.base.login.event;

import com.slg.common.event.model.IEvent;
import com.slg.game.base.player.model.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 玩家正式登录事件
 * 在客户端发送 CM_LoginFinish 后、玩家线程中抛出
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class PlayerLoginEvent implements IEvent {

    private Player player;
}
