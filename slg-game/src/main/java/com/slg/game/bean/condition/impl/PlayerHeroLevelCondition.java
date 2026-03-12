package com.slg.game.bean.condition.impl;

import com.slg.sharedmodules.progress.model.ProgressMeta;
import com.slg.game.base.player.model.Player;
import com.slg.game.bean.condition.IPlayerProgressCondition;
import com.slg.game.develop.hero.event.HeroLevelUpEvent;
import com.slg.game.develop.hero.model.HeroPlayerInfo;
import com.slg.table.anno.TableBean;
import lombok.Getter;
import lombok.Setter;

/**
 * 玩家有{1}个等级{2}的英雄
 *
 * @author yangxunan
 * @date 2026/1/29
 */
@TableBean
@Getter
@Setter
public class PlayerHeroLevelCondition implements IPlayerProgressCondition<HeroLevelUpEvent> {

    private int level;

    private int num;

    @Override
    public void init(Player owner, ProgressMeta meta){
        HeroPlayerInfo heroPlayerInfo = owner.getPlayerEntity().getHeroPlayerInfo();
        int playerNum = Math.toIntExact(heroPlayerInfo.getHeros().values().stream().filter(h -> h.getLevel() >= level).count());
        meta.setProgress(playerNum);
    }

    @Override
    public void onEvent(HeroLevelUpEvent event, ProgressMeta meta){

        if (event.getLevel() < level){
            return;
        }
        Player player = event.getPlayer();
        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        int playerNum = Math.toIntExact(heroPlayerInfo.getHeros().values().stream().filter(h -> h.getLevel() >= level).count());
        meta.setProgress(playerNum);
    }

    @Override
    public long getFinishProgress(){
        return num;
    }

    @Override
    public boolean verify(Player player){

        HeroPlayerInfo heroPlayerInfo = player.getPlayerEntity().getHeroPlayerInfo();
        int playerNum = Math.toIntExact(heroPlayerInfo.getHeros().values().stream().filter(h -> h.getLevel() >= level).count());
        return playerNum >= num;
    }
}
