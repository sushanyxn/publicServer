package com.slg.game.bean.reward.impl;

import com.slg.common.bean.RewardResult;
import com.slg.common.constant.CurrencyType;
import com.slg.game.base.player.model.Player;
import com.slg.game.bean.reward.IPlayerReward;
import com.slg.table.anno.TableBean;

import java.util.Map;

/**
 * @author yangxunan
 * @date 2026/1/29
 */
@TableBean
public class CurrencyReward implements IPlayerReward {

    private Map<CurrencyType, Long> rewards;

    @Override
    public RewardResult reward(Player player){
        return null;
    }

    @Override
    public RewardResult reward(Player player, float rate){
        return null;
    }
}
