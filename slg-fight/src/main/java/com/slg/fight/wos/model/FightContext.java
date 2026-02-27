package com.slg.fight.wos.model;

import lombok.Data;

/**
 * 单场战斗上下文，供结算流程读写。
 *
 * @author yangxunan
 * @date 2026-02-05
 */
@Data
public class FightContext {

    /** 攻方军队 */
    private FightArmy attacker;

    /** 守方军队 */
    private FightArmy defender;

    /** 本场战斗记录 */
    private FightRecord fightRecord;

    /** 进攻方胜利 */
    private boolean attackerWin;

    /** 战报 VO，由场景层在战后填充，不参与序列化 */
    private Object report;

    /**
     * 由攻守方构造战斗上下文，战斗记录初始为空。
     *
     * @param attacker 进攻方军队，非 null
     * @param defender 防守方军队，非 null
     * @return FightContext 实例
     */
    public static FightContext valueOf(FightArmy attacker, FightArmy defender) {
        FightContext ctx = new FightContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        ctx.setFightRecord(new FightRecord());
        return ctx;
    }
}
