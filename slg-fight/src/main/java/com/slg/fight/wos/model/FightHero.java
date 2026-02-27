package com.slg.fight.wos.model;

import com.slg.net.message.clientmessage.report.packet.FightHeroVO;
import lombok.Data;

/**
 * 战斗中单个英雄的参战数据。
 *
 * @author yangxunan
 * @date 2026-02-05
 */
@Data
public class FightHero {

    /** 英雄配置 id */
    private int heroId;

    /** 英雄等级 */
    private int heroLv;

    /**
     * 构造单个英雄参战数据
     *
     * @param heroId 英雄配置 id
     * @param heroLv 英雄等级
     * @return FightHero 实例
     */
    public static FightHero valueOf(int heroId, int heroLv) {
        FightHero h = new FightHero();
        h.setHeroId(heroId);
        h.setHeroLv(heroLv);
        return h;
    }

    /**
     * 转为战报用英雄简要 VO。
     *
     * @return FightHeroVO，用于战报展示
     */
    public FightHeroVO toFightHeroVO() {
        FightHeroVO vo = new FightHeroVO();
        vo.setHeroId(heroId);
        vo.setHeroLv(heroLv);
        return vo;
    }
}
