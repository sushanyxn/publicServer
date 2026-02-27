package com.slg.game.develop.hero.event;

import com.slg.game.base.player.model.Player;
import com.slg.game.bean.event.IPlayerProgressEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 英雄升级事件
 *
 * @author yangxunan
 * @date 2026/1/29
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class HeroLevelUpEvent implements IPlayerProgressEvent {

    private Player player;

    private int heroId;

    private int level;

    @Override
    public long getOwnerId(){
        return player.getId();
    }
}
