package com.slg.game.develop.hero.service;

import com.slg.common.log.LoggerUtil;
import com.slg.game.develop.hero.table.HeroLevelTable;
import com.slg.game.develop.hero.table.HeroTable;
import com.slg.table.anno.Table;
import com.slg.table.model.TableInt;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 英雄管理器
 * 展示单字段索引和复合索引的使用
 * 
 * @author yangxunan
 * @date 2026/01/04
 */
@Component
@Getter
public class HeroManager {

    @Table
    private TableInt<HeroTable> heroTables;

    @Table
    private TableInt<HeroLevelTable> heroLevelTables;

    @PostConstruct
    public void init(){
        // 编程式注册热更新监听器
        heroLevelTables.addReloadListener(() -> {
            LoggerUtil.debug("【编程式】HeroLevelTable 热更新完成，执行业务逻辑");
        });
    }

    /**
     * 根据ID获取英雄配置
     */
    public HeroTable getHeroTableById(int id) {
        return heroTables.get(id);
    }

    /**
     * 根据英雄ID和等级获取等级配置（使用复合索引）
     */
    public HeroLevelTable getHeroLevelTableByHeroAndLv(int heroId, int level) {
        // 使用复合索引精确查询
        return heroLevelTables.getOneByCompositeIndex(
            HeroLevelTable.IDX_HERO_LEVEL, heroId, level);
    }

    /**
     * 获取指定英雄的所有等级配置（使用单字段索引）
     */
    public Collection<HeroLevelTable> getHeroAllLevels(int heroId) {
        return heroLevelTables.getIndexd(HeroLevelTable.INDEX_HERO_ID, heroId);
    }

}
