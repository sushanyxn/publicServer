package com.slg.net.message.clientmessage.hero.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 英雄信息全量推送
 * 登录时服务端推送给客户端的全部英雄数据
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class SM_HeroInfo {

    /** 玩家拥有的所有英雄 */
    private HeroVO[] heroes;

}
