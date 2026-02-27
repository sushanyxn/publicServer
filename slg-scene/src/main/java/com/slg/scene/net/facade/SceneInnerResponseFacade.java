package com.slg.scene.net.facade;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.message.innermessage.socket.packet.IM_RegisterSessionResponce;
import com.slg.net.socket.model.NetSession;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/2/24
 */
@Component
public class SceneInnerResponseFacade {

    @MessageHandler
    public void socketRegisterResponse(NetSession netSession, IM_RegisterSessionResponce response) {
        if (response.getResult() == 0) {
            LoggerUtil.debug("与服务器{}的连接建立完成", netSession.getServerId());
        }
    }

}
