package com.slg.client.config;

import com.slg.table.anno.Table;
import com.slg.table.anno.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户端英雄配置表
 * 比服务端多一个 name 字段，用于 UI 展示
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Table(alias = "HeroTable")
@Getter
@Setter
public class ClientHeroTable {

    @TableId
    private int id;

    private int type;

    /** 英雄名称（仅客户端使用） */
    private String name;

}
