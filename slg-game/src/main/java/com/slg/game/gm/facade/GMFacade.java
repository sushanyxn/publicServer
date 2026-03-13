package com.slg.game.gm.facade;

import com.slg.game.base.player.model.Player;
import com.slg.game.gm.service.GMService;
import com.slg.net.message.clientmessage.gm.packet.CM_GMCommand;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * GM 模块 Facade
 * 处理 CM_GMCommand 协议，委托 GMService 执行指令并推送结果
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class GMFacade {

    @Autowired
    private GMService gmService;

    @MessageHandler
    public void gmCommand(NetSession session, CM_GMCommand req, Player player) {
        gmService.executeCommand(player, req.getCommand());
    }

}
