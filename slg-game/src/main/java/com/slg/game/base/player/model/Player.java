package com.slg.game.base.player.model;

import com.slg.game.base.player.entity.PlayerEntity;
import com.slg.net.message.clientmessage.scene.packet.ScenePlayerVO;
import com.slg.net.socket.model.NetSession;
import lombok.Getter;

/**
 * @author yangxunan
 * @date 2025/12/23
 */
@Getter
public class Player {

    private long id;

    private PlayerEntity playerEntity;

    private LoginContext loginContext;

    private SceneContext sceneContext;


    public Player(PlayerEntity playerEntity){
        this.id = playerEntity.getId();
        this.playerEntity = playerEntity;
        this.loginContext = new LoginContext(this);
        this.sceneContext = new SceneContext(this);

    }

    public void bindSession(NetSession session){
        loginContext.bind(session);
    }

    public void unbindSession(NetSession session){
        loginContext.unbind(session);
    }

    public boolean isOnline(){
        return loginContext.isOnline();
    }

    public NetSession getSession(){
        return loginContext.getSession();
    }

    public ScenePlayerVO toScenePlayerVO(){
        ScenePlayerVO scenePlayerVO = new ScenePlayerVO();
        scenePlayerVO.setPlayerId(id);
        scenePlayerVO.setGameServerId(playerEntity.getServerId());
        return  scenePlayerVO;
    }

}
