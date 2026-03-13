package com.slg.client.message.gm;

import com.slg.client.core.account.ClientAccount;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.clientmessage.gm.packet.SM_GMResult;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import org.springframework.stereotype.Component;

/**
 * 客户端 GM 消息处理
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class GMClientHandler {

    public static final String MODULE_KEY = "gm_log";

    @MessageHandler
    public void onGMResult(NetSession session, SM_GMResult msg, ClientAccount account) {
        String status = msg.getCode() == 0 ? "成功" : "失败";
        String line = "< [" + status + "] " + msg.getMessage();

        LoggerUtil.info("账号 {} GM 结果: {} -> code={}, {}", account.getAccount(), msg.getCommand(), msg.getCode(), msg.getMessage());

        SimpleStringProperty logProperty = getOrCreateLog(account);
        Platform.runLater(() -> logProperty.set(logProperty.get() + line + "\n"));
    }

    public static SimpleStringProperty getOrCreateLog(ClientAccount account) {
        SimpleStringProperty logProperty = account.getModuleData(MODULE_KEY, SimpleStringProperty.class);
        if (logProperty == null) {
            logProperty = new SimpleStringProperty("");
            account.setModuleData(MODULE_KEY, logProperty);
        }
        return logProperty;
    }

}
