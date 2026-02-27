package com.slg.game.develop.hero.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author yangxunan
 * @date 2026/1/21
 */
@Getter
@Setter
public class HeroInfo {

    private int heroId;
    private int level;

    public static HeroInfo valueOf(int heroId, int level) {
        HeroInfo hero = new HeroInfo();
        hero.heroId = heroId;
        hero.level = level;
        return hero;
    }

}
