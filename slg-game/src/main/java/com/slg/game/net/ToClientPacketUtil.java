package com.slg.game.net;

import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.game.base.player.model.Player;
import com.slg.net.socket.model.NetSession;

/**
 * @author yangxunan
 * @date 2026/1/22
 */
public class ToClientPacketUtil {

    public static void send(Player player, Object packet){
        NetSession session = player.getSession();
        if (session == null || !session.isActive()){
            return;
        }
        LoggerUtil.info("[发] playerId={} {} {}", player.getId(), packet.getClass().getSimpleName(), JsonUtil.toJson(packet));
        session.sendMessage(packet);
    }

}
