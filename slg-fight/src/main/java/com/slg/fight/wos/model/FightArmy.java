package com.slg.fight.wos.model;

import com.slg.net.message.clientmessage.report.packet.FightHeroVO;
import com.slg.net.message.clientmessage.report.packet.FightTroopVO;
import lombok.Data;

/**
 * 战斗一方军队数据（攻方/守方阵容）。
 *
 * @author yangxunan
 * @date 2026-02-05
 */
@Data
public class FightArmy {

    /** 参战英雄列表 */
    private FightHero[] heroes;

    /** 参战兵种列表 */
    private FightTroop[] troops;

    /**
     * 由英雄与兵种列表构造单军队数据。
     * 英雄、兵种为 null 或空时使用空数组；不允许返回 null，双方均为空时也返回 heroes/troops 为空数组的实例。
     *
     * @param heroes 参战英雄，可为 null 或空
     * @param troops 参战兵种，可为 null 或空
     * @return FightArmy，永不为 null
     */
    public static FightArmy valueOf(FightHero[] heroes, FightTroop[] troops) {
        boolean noHeroes = heroes == null || heroes.length == 0;
        boolean noTroops = troops == null || troops.length == 0;
        FightArmy army = new FightArmy();
        army.setHeroes(noHeroes ? new FightHero[0] : heroes);
        army.setTroops(noTroops ? new FightTroop[0] : troops);
        return army;
    }

    /**
     * 将本军英雄列表转为战报用英雄 VO 数组。
     *
     * @return FightHeroVO 数组，无英雄时返回空数组
     */
    public FightHeroVO[] toFightHeroVOs() {
        if (heroes == null || heroes.length == 0) {
            return new FightHeroVO[0];
        }
        FightHeroVO[] result = new FightHeroVO[heroes.length];
        for (int i = 0; i < heroes.length; i++) {
            result[i] = heroes[i].toFightHeroVO();
        }
        return result;
    }

    /**
     * 将本军兵种列表转为战报用兵种 VO 数组。
     *
     * @return FightTroopVO 数组，无兵种时返回空数组
     */
    public FightTroopVO[] toFightTroopVOs() {
        if (troops == null || troops.length == 0) {
            return new FightTroopVO[0];
        }
        FightTroopVO[] result = new FightTroopVO[troops.length];
        for (int i = 0; i < troops.length; i++) {
            result[i] = troops[i].toFightTroopVO();
        }
        return result;
    }
}
