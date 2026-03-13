package com.slg.net.message.clientmessage.hero.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 英雄变更增量推送
 * 英雄获得或升级时服务端推送给客户端的单个英雄数据
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "valueOf")
public class SM_HeroUpdate {

    /** 变更的英雄数据 */
    private HeroVO hero;

}
