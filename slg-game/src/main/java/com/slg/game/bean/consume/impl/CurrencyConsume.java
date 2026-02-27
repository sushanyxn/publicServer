package com.slg.game.bean.consume.impl;

import com.slg.common.constant.CurrencyType;
import com.slg.game.base.player.model.Player;
import com.slg.game.bean.consume.IPlayerConsume;
import com.slg.table.anno.TableBean;

import java.util.Map;

/**
 * @author yangxunan
 * @date 2026/1/14
 */
@TableBean
public class CurrencyConsume implements IPlayerConsume {

    private Map<CurrencyType, Long> currencies;

    @Override
    public boolean verify(Player player){
        return true;
    }

    @Override
    public void verifyThrow(Player player){
    }

    @Override
    public void consume(Player player){

    }
}
