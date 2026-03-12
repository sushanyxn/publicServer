package com.slg.sharedmodules.fight.wos.model;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 多军队战斗数据，继承 {@link FightArmy}。
 * 父类中的 {@link #getHeroes()} 为队长英雄，{@link #getTroops()} 为所有人聚合后的兵种；
 * 同时保留各成员的单军队数据映射（playerId -> FightArmy）。
 *
 * @author yangxunan
 * @date 2026-02-05
 */
@Getter
public class MultiFightArmy extends FightArmy {

    /** 各成员对应的单军队数据，key 为 playerId */
    private final Map<Long, FightArmy> members;

    /**
     * 由多支单军队（按 playerId 索引）与队长 id 构造多军队战斗数据。
     * 父类 heroes 取队长的英雄，troops 取所有成员兵种按 troopId 聚合后的结果。
     *
     * @param memberArmies playerId -> FightArmy，非 null
     * @param leaderId     队长玩家 id
     * @return MultiFightArmy 实例
     */
    public static MultiFightArmy valueOf(Map<Long, FightArmy> memberArmies, long leaderId) {
        Map<Long, FightArmy> copy = memberArmies == null || memberArmies.isEmpty()
                ? Collections.emptyMap()
                : new HashMap<>(memberArmies);
        MultiFightArmy multi = new MultiFightArmy(copy);
        FightArmy leaderArmy = copy.get(leaderId);
        FightHero[] leaderHeroes = (leaderArmy != null && leaderArmy.getHeroes() != null && leaderArmy.getHeroes().length > 0)
                ? leaderArmy.getHeroes() : new FightHero[0];
        multi.setHeroes(leaderHeroes);
        multi.setTroops(mergeTroopsFromMap(copy));
        return multi;
    }

    private static FightTroop[] mergeTroopsFromMap(Map<Long, FightArmy> armies) {
        Map<Integer, int[]> merge = new HashMap<>();
        for (FightArmy a : armies.values()) {
            if (a.getTroops() == null) {
                continue;
            }
            for (FightTroop t : a.getTroops()) {
                merge.merge(t.getTroopId(),
                        new int[]{t.getTroopNum(), t.getTroopInitNum(), t.getTroopHurtNum()},
                        (x, y) -> new int[]{x[0] + y[0], x[1] + y[1], x[2] + y[2]});
            }
        }
        if (merge.isEmpty()) {
            return new FightTroop[0];
        }
        return merge.entrySet().stream()
                .map(e -> FightTroop.valueOf(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toArray(FightTroop[]::new);
    }

    private MultiFightArmy(Map<Long, FightArmy> members) {
        this.members = Collections.unmodifiableMap(members);
    }

}
