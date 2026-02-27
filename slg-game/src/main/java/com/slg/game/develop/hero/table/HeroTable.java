package com.slg.game.develop.hero.table;

import com.slg.table.anno.Table;
import com.slg.table.anno.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 英雄表
 * @author yangxunan
 * @date 2025/12/26
 */
@Table
@Getter
@Setter
public class HeroTable {

    /**
     * 主键
     */
    @TableId
    private int id;

    private int type;

}
