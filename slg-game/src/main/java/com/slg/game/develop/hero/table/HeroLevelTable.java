package com.slg.game.develop.hero.table;

import com.slg.common.bean.AndConsume;
import com.slg.game.base.player.model.Player;
import com.slg.game.bean.consume.IPlayerConsume;
import com.slg.table.anno.*;
import com.slg.table.extend.TablePostProcessor;
import lombok.Getter;
import lombok.Setter;

/**
 * 英雄等级配置表
 * 
 * @author yangxunan
 * @date 2025-12-26
 */
@Table
@Getter
@Setter
public class HeroLevelTable implements TablePostProcessor {

    /**
     * 单字段索引：通过英雄ID查询
     */
    public static final String INDEX_HERO_ID = "INDEX_HERO_ID";
    
    /**
     * 复合索引：通过英雄ID + 等级查询（唯一）
     */
    public static final String IDX_HERO_LEVEL = "IDX_HERO_LEVEL";

    @TableId
    private int id;

    /**
     * 1. 英雄ID索引
     * 2. 单字段索引 + 复合索引第一个字段
     * 3. Hero表关联检查
     */
    @TableIndex(INDEX_HERO_ID)
    @TableCompositeIndexField(name = IDX_HERO_LEVEL, order = 0, unique = true)
    @TableRefCheck(HeroTable.class)
    private int heroId;

    /**
     * 等级
     * 复合索引第二个字段
     */
    @TableCompositeIndexField(name = IDX_HERO_LEVEL, order = 1)
    private int level;

    /**
     * 升级消耗
     */
    private IPlayerConsume[] consumes;

    /**
     * 攻击力
     */
    private int atk;

    /**
     * 防御力
     */
    private int def;

    /**
     * 生命值
     */
    private int hp;

    /**
     * 聚合条件
     */
    private transient AndConsume<Player> consume;

    @Override
    public void postProcessAfterInitialization(){
        AndConsume<Player> consume = new AndConsume<>();
        if (consumes != null) {
            for (IPlayerConsume iPlayerConsume : consumes) {
                consume.addConsume(iPlayerConsume);
            }
        }
        this.consume = consume;
    }

}
