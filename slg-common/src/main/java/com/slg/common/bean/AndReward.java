package com.slg.common.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合奖励
 *
 * @author yangxunan
 * @date 2026/1/29
 */
public class AndReward<T> implements IReward<T> {

    private List<IReward<T>> rewards = new ArrayList<IReward<T>>();

    public void addReward(IReward<T> reward){
        rewards.add(reward);
    }

    @Override
    public RewardResult reward(T t){
        RewardResult rewardResult = new RewardResult();
        for (IReward<T> reward : rewards) {
            rewardResult.merge(reward.reward(t));
        }
        return rewardResult;
    }

    @Override
    public RewardResult reward(T t, float rate){
        RewardResult rewardResult = new RewardResult();
        for (IReward<T> reward : rewards) {
            rewardResult.merge(reward.reward(t, rate));
        }
        return rewardResult;
    }
}
