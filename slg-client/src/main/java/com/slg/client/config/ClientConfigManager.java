package com.slg.client.config;

import com.slg.table.anno.Table;
import com.slg.table.model.TableInt;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * 客户端配置表管理器
 * 集中管理客户端使用的所有配置表
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
@Getter
public class ClientConfigManager {

    @Table
    private TableInt<ClientHeroTable> heroTables;

    public ClientHeroTable getHeroTable(int heroId) {
        return heroTables.get(heroId);
    }

}
