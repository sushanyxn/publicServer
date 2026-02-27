package com.slg.game.develop.hero.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yangxunan
 * @date 2026/1/21
 */
@Getter
@Setter
public class HeroPlayerInfo {

    private Map<Integer, HeroInfo> heros = new HashMap<>();

}
