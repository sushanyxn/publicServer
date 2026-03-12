package com.slg.sharedmodules.fight.wos;

import com.slg.sharedmodules.fight.wos.model.FightArmy;
import com.slg.sharedmodules.fight.wos.model.FightContext;

/**
 * 战斗任务提交入口。
 * 接收进攻方与防守方军队数据，返回本场战斗上下文，供后续结算或异步执行使用。
 *
 * @author yangxunan
 * @date 2026-02-05
 */
public final class FightTaskSubmit {

    private FightTaskSubmit() {
    }

    /**
     * 提交一场战斗任务：由进攻方与防守方构造战斗上下文并返回。
     *
     * @param attacker 进攻方军队，非 null
     * @param defender 防守方军队，非 null
     * @return 本场战斗上下文（含空战斗记录，供结算流程写入）
     */
    public static FightContext submit(FightArmy attacker, FightArmy defender) {
        return FightContext.valueOf(attacker, defender);
    }
}
