package com.slg.game.gm.command;

import com.slg.game.base.player.model.Player;
import com.slg.game.develop.hero.model.HeroInfo;
import com.slg.game.develop.hero.model.HeroPlayerInfo;
import com.slg.game.develop.hero.service.HeroService;
import com.slg.game.gm.model.IGMCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.slg.game.gm.service.GMService.FAIL;
import static com.slg.game.gm.service.GMService.SUCCESS;

/**
 * 英雄相关 GM 指令
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class HeroGMCommand implements IGMCommand {

    @Autowired
    private HeroService heroService;

    /**
     * 获得指定英雄
     * 用法: gainHero 1001
     */
    public int gainHero(Player player, int heroId) {
        heroService.gainHero(player, heroId);
        return SUCCESS;
    }

    /**
     * 获得全部英雄
     * 用法: gainAllHero
     */
    public int gainAllHero(Player player) {
        heroService.gmGainAllHeros(player);
        return SUCCESS;
    }

    /**
     * 全部英雄满级
     * 用法: heroMaxLevel
     */
    public int heroMaxLevel(Player player) {
        heroService.gmHeroMaxLevel(player);
        return SUCCESS;
    }

    /**
     * 设置指定英雄等级
     * 用法: setHeroLevel 1001 3
     */
    public int setHeroLevel(Player player, int heroId, int level) {
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        HeroInfo heroInfo = heroPlayerInfo.getHeros().get(heroId);
        if (heroInfo == null) {
            return FAIL;
        }
        heroInfo.setLevel(level);
        heroService.saveHeroInfo(player);
        heroService.pushHeroUpdate(player, heroInfo);
        return SUCCESS;
    }

}
