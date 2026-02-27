package com.slg.game.develop.hero.service;

import com.slg.common.event.manager.EventBusManager;
import com.slg.game.base.player.entity.PlayerEntity;
import com.slg.game.base.player.model.Player;
import com.slg.game.develop.hero.event.HeroLevelUpEvent;
import com.slg.game.develop.hero.model.HeroPlayerInfo;
import com.slg.game.develop.hero.model.HeroInfo;
import com.slg.game.develop.hero.table.HeroLevelTable;
import com.slg.game.develop.hero.table.HeroTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/1/21
 */
@Component
public class HeroService {

    @Autowired
    private HeroManager heroManager;

    public void onLogin(Player player){

    }

    public void gainHero(Player player, int heroId){
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        heroPlayerInfo.getHeros().putIfAbsent(heroId, HeroInfo.valueOf(heroId, 1));

        saveHeroInfo(player);
    }

    public void levelUpHero(Player player, int heroId){
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        HeroInfo heroInfo = heroPlayerInfo.getHeros().get(heroId);
        if (heroInfo == null){
            return;
        }
        HeroLevelTable heroLevelTable = heroManager.getHeroLevelTableByHeroAndLv(heroInfo.getHeroId(), heroInfo.getLevel() + 1);
        if (heroLevelTable == null){
            // 等级配置不存在
            return;
        }

        // 判断消耗
        heroLevelTable.getConsume().verify(player);
        // 正式消耗
        heroLevelTable.getConsume().consume(player);
        // 升级
        heroInfo.setLevel(heroInfo.getLevel() + 1);
        saveHeroInfo(player);

        // 抛出英雄升级事件
        EventBusManager.getInstance().publishEvent(HeroLevelUpEvent.valueOf(player, heroId, heroInfo.getLevel()));
    }

    public void gmGainAllHeros(Player player){
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        for (HeroTable heroTable : heroManager.getHeroTables().getAll()) {
            heroPlayerInfo.getHeros().putIfAbsent(heroTable.getId(), HeroInfo.valueOf(heroTable.getId(), 1));
        }
        saveHeroInfo(player);
    }

    public void gmHeroMaxLevel(Player player){
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        for (HeroInfo hero : heroPlayerInfo.getHeros().values()) {
            hero.setLevel(5);
        }
        saveHeroInfo(player);
    }



    public void saveHeroInfo(Player player){
        player.getPlayerEntity().saveField(PlayerEntity.Fields.heroPlayerInfo);
    }

}
