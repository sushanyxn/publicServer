package com.slg.game.develop.hero.facade;

import com.slg.common.event.anno.EventListener;
import com.slg.game.base.login.event.PlayerLoginEvent;
import com.slg.game.base.player.model.Player;
import com.slg.game.develop.hero.service.HeroService;
import com.slg.net.message.clientmessage.hero.packet.CM_HeroLevelUp;
import com.slg.net.message.core.anno.MessageHandler;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 英雄模块 Facade
 * 处理英雄相关的客户端协议，监听登录事件下发英雄数据
 *
 * @author yangxunan
 * @date 2026/1/21
 */
@Component
public class HeroFacade {

    @Autowired
    private HeroService heroService;

    /**
     * 登录完成时推送全量英雄数据
     */
    @EventListener
    public void onPlayerLogin(PlayerLoginEvent event) {
        heroService.onLogin(event.getPlayer());
    }

    /**
     * 处理英雄升级请求
     */
    @MessageHandler
    public void heroLevelUp(NetSession session, CM_HeroLevelUp req, Player player) {
        heroService.levelUpHero(player, req.getHeroId());
    }

}
