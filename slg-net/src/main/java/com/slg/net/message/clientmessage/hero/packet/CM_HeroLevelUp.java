package com.slg.net.message.clientmessage.hero.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 英雄升级请求
 * 客户端发送给服务端，请求对指定英雄进行升级
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
@Setter
public class CM_HeroLevelUp {

    /** 要升级的英雄配置 id */
    private int heroId;

}
