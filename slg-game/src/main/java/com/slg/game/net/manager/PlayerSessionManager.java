package com.slg.game.net.manager;

import com.slg.game.SpringContext;
import com.slg.game.base.player.model.Player;
import com.slg.common.executor.Executor;
import com.slg.net.socket.model.NetSession;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yangxunan
 * @date 2026/1/22
 */
@Component
public class PlayerSessionManager {

    @Getter
    private Map<NetSession, Player> onlinePlayers = new ConcurrentHashMap<>();

    public void bindPlayer(NetSession session, Player player){
        if (player.getSession() != null) {
            unbindPlayer(player.getSession());
        }
        onlinePlayers.put(session, player);
        player.bindSession(session);
    }

    public void unbindPlayer(NetSession session){
        Player player = onlinePlayers.remove(session);
        if (player != null && player.getSession() == session) {
            player.unbindSession(session);
        }
    }

    public Collection<Player> getOnlinePlayers(){
        return onlinePlayers.values();
    }

    public void shutdown(){
        Executor.Login.execute(() -> {
            for (NetSession session : onlinePlayers.keySet()) {
                SpringContext.getLoginService().logout(session);
                session.close("服务器关闭");
            }
            onlinePlayers.clear();
        });
    }

}
