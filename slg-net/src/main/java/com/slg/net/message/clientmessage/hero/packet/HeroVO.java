package com.slg.net.message.clientmessage.hero.packet;

import lombok.Data;

/**
 * 英雄 VO
 *
 * @author yangxunan
 * @date 2026/2/5
 */
@Data
public class HeroVO {

    /** 英雄配置 id */
    private int heroId;

    /** 英雄等级 */
    private int heroLv;

}
