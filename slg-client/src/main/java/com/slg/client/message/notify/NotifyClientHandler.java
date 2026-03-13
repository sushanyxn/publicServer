package com.slg.client.message.notify;

import com.slg.client.config.ClientConfigManager;
import com.slg.client.config.ClientMessageTable;
import com.slg.client.core.account.ClientAccount;
import com.slg.client.ui.TipManager;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.clientmessage.notify.packet.SM_NotifyMessage;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 客户端通知消息处理
 * 收到服务端推送的 SM_NotifyMessage 后，根据 infoId 查找消息内容并弹 tips
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class NotifyClientHandler {

    @Autowired
    private ClientConfigManager configManager;

    @MessageHandler
    public void onNotifyMessage(NetSession session, SM_NotifyMessage msg, ClientAccount account) {
        int infoId = msg.getInfoId();
        ClientMessageTable messageTable = configManager.getMessageTable(infoId);

        String content;
        if (messageTable != null) {
            content = messageTable.getContent();
        } else {
            content = "未知消息 (id=" + infoId + ")";
        }

        LoggerUtil.info("账号 {} 收到通知: infoId={}, type={}, content={}",
                account.getAccount(), infoId, msg.getMsgType(), content);

        TipManager.showTip(content);
    }

}
