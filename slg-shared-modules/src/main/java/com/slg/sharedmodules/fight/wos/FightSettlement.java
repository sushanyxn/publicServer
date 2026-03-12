package com.slg.sharedmodules.fight.wos;

import com.slg.sharedmodules.fight.wos.model.FightArmy;
import com.slg.sharedmodules.fight.wos.model.FightContext;
import com.slg.sharedmodules.fight.wos.model.FightTroop;

/**
 * 战斗结算
 * <p>根据战斗上下文执行结算逻辑，设置 {@link FightContext#setAttackerWin(boolean)}。</p>
 *
 * @author yangxunan
 * @date 2026-02-06
 */
public final class FightSettlement {

    private FightSettlement() {
    }

    /**
     * 对已提交的战斗上下文执行结算，写入进攻方是否胜利。
     * <p>当前为简单规则：按双方总兵力（英雄数 + 兵种数量之和）比较，进攻方总兵力 ≥ 防守方时进攻方胜利。</p>
     *
     * @param ctx 战斗上下文，非 null
     */
    public static void settle(FightContext ctx) {
        if (ctx == null) {
            return;
        }
        long attackerPower = power(ctx.getAttacker());
        long defenderPower = power(ctx.getDefender());
        ctx.setAttackerWin(attackerPower >= defenderPower);
    }

    private static long power(FightArmy army) {
        if (army == null) {
            return 0;
        }
        long p = 0;
        if (army.getHeroes() != null) {
            p += army.getHeroes().length;
        }
        if (army.getTroops() != null) {
            for (FightTroop t : army.getTroops()) {
                if (t != null) {
                    p += t.getTroopNum();
                }
            }
        }
        return p;
    }
}
