package com.slg.client.message.hero;

import com.slg.client.core.account.ClientAccount;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.clientmessage.hero.packet.HeroVO;
import com.slg.net.message.clientmessage.hero.packet.SM_HeroInfo;
import com.slg.net.message.clientmessage.hero.packet.SM_HeroUpdate;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.stereotype.Component;

/**
 * 客户端英雄消息处理
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class HeroClientHandler {

    public static final String MODULE_KEY = "hero";

    /**
     * 处理登录时全量英雄推送
     */
    @MessageHandler
    public void onHeroInfo(NetSession session, SM_HeroInfo msg, ClientAccount account) {
        @SuppressWarnings("unchecked")
        ObservableList<HeroVO> heroList = account.getModuleData(MODULE_KEY, ObservableList.class);
        if (heroList == null) {
            heroList = FXCollections.observableArrayList();
            account.setModuleData(MODULE_KEY, heroList);
        }

        ObservableList<HeroVO> finalList = heroList;
        Platform.runLater(() -> {
            finalList.clear();
            if (msg.getHeroes() != null) {
                finalList.addAll(msg.getHeroes());
            }
        });

        LoggerUtil.info("账号 {} 收到英雄全量数据，数量: {}",
                account.getAccount(), msg.getHeroes() != null ? msg.getHeroes().length : 0);
    }

    /**
     * 处理单个英雄变更推送
     */
    @MessageHandler
    public void onHeroUpdate(NetSession session, SM_HeroUpdate msg, ClientAccount account) {
        HeroVO updated = msg.getHero();
        if (updated == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        ObservableList<HeroVO> heroList = account.getModuleData(MODULE_KEY, ObservableList.class);
        if (heroList == null) {
            heroList = FXCollections.observableArrayList();
            account.setModuleData(MODULE_KEY, heroList);
        }

        ObservableList<HeroVO> finalList = heroList;
        Platform.runLater(() -> {
            boolean found = false;
            for (int i = 0; i < finalList.size(); i++) {
                if (finalList.get(i).getHeroId() == updated.getHeroId()) {
                    finalList.set(i, updated);
                    found = true;
                    break;
                }
            }
            if (!found) {
                finalList.add(updated);
            }
        });

        LoggerUtil.info("账号 {} 英雄变更: heroId={}, lv={}",
                account.getAccount(), updated.getHeroId(), updated.getHeroLv());
    }
}
