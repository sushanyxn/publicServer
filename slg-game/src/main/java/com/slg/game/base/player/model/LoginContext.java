package com.slg.game.base.player.model;

import com.slg.net.socket.model.NetSession;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yangxunan
 * @date 2025/12/23
 */
@Getter
@Setter
public class LoginContext {

    private Player player;

    private NetSession session;

    public LoginContext(Player player) {
        this.player = player;
    }

    public boolean isOnline(){
        return session != null;
    }

    public void bind(NetSession session){
        this.session = session;
        session.setPlayerId(player.getId());
    }

    public void unbind(NetSession session){
        this.session = null;
        session.setPlayerId(0);
    }
}
