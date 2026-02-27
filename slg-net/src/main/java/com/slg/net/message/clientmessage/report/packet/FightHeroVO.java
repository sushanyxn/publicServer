package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;

/**
 * 战斗英雄简要 VO。
 * 描述单名参战英雄的配置 id 与等级，用于战报展示。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
public class FightHeroVO {

    /** 英雄配置 id */
    private int heroId;

    /** 英雄等级 */
    private int heroLv;

}
