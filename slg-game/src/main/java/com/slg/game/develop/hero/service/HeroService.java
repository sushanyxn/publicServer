package com.slg.game.develop.hero.service;

import com.slg.common.event.manager.EventBusManager;
import com.slg.game.base.player.entity.PlayerEntity;
import com.slg.game.base.player.model.Player;
import com.slg.game.develop.hero.event.HeroLevelUpEvent;
import com.slg.game.develop.hero.model.HeroPlayerInfo;
import com.slg.game.develop.hero.model.HeroInfo;
import com.slg.game.develop.hero.table.HeroLevelTable;
import com.slg.game.develop.hero.table.HeroTable;
import com.slg.game.net.ToClientPacketUtil;
import com.slg.net.message.clientmessage.hero.packet.HeroVO;
import com.slg.net.message.clientmessage.hero.packet.SM_HeroInfo;
import com.slg.net.message.clientmessage.hero.packet.SM_HeroUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 英雄业务服务
 *
 * @author yangxunan
 * @date 2026/1/21
 */
@Component
public class HeroService {

    @Autowired
    private HeroManager heroManager;

    /**
     * 登录时推送全量英雄数据
     */
    public void onLogin(Player player) {
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        Map<Integer, HeroInfo> heros = heroPlayerInfo.getHeros();

        HeroVO[] heroVOs = heros.values().stream()
                .map(this::toHeroVO)
                .toArray(HeroVO[]::new);

        ToClientPacketUtil.send(player, SM_HeroInfo.valueOf(heroVOs));
    }

    /**
     * 获得英雄
     */
    public void gainHero(Player player, int heroId) {
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        HeroInfo heroInfo = heroPlayerInfo.getHeros().get(heroId);
        if (heroInfo != null) {
            return;
        }

        heroInfo = HeroInfo.valueOf(heroId, 1);
        heroPlayerInfo.getHeros().put(heroId, heroInfo);
        saveHeroInfo(player);

        pushHeroUpdate(player, heroInfo);
    }

    /**
     * 英雄升级
     */
    public void levelUpHero(Player player, int heroId) {
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        HeroInfo heroInfo = heroPlayerInfo.getHeros().get(heroId);
        if (heroInfo == null) {
            return;
        }
        HeroLevelTable heroLevelTable = heroManager.getHeroLevelTableByHeroAndLv(heroInfo.getHeroId(), heroInfo.getLevel() + 1);
        if (heroLevelTable == null) {
            return;
        }

        heroLevelTable.getConsume().verify(player);
        heroLevelTable.getConsume().consume(player);

        heroInfo.setLevel(heroInfo.getLevel() + 1);
        saveHeroInfo(player);

        pushHeroUpdate(player, heroInfo);

        EventBusManager.getInstance().publishEvent(HeroLevelUpEvent.valueOf(player, heroId, heroInfo.getLevel()));
    }

    /**
     * GM：获得全部英雄
     */
    public void gmGainAllHeros(Player player) {
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        for (HeroTable heroTable : heroManager.getHeroTables().getAll()) {
            heroPlayerInfo.getHeros().putIfAbsent(heroTable.getId(), HeroInfo.valueOf(heroTable.getId(), 1));
        }
        saveHeroInfo(player);
        onLogin(player);
    }

    /**
     * GM：全部英雄满级
     */
    public void gmHeroMaxLevel(Player player) {
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        for (HeroInfo hero : heroPlayerInfo.getHeros().values()) {
            hero.setLevel(5);
        }
        saveHeroInfo(player);
        onLogin(player);
    }

    /**
     * 推送单个英雄变更（增量）
     */
    public void pushHeroUpdate(Player player, HeroInfo heroInfo) {
        ToClientPacketUtil.send(player, SM_HeroUpdate.valueOf(toHeroVO(heroInfo)));
    }

    private HeroVO toHeroVO(HeroInfo heroInfo) {
        HeroVO vo = new HeroVO();
        vo.setHeroId(heroInfo.getHeroId());
        vo.setHeroLv(heroInfo.getLevel());
        return vo;
    }

    public void saveHeroInfo(Player player) {
        player.getPlayerEntity().saveField(PlayerEntity.Fields.heroPlayerInfo);
    }

}
