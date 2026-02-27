package com.slg.game.base.player.service;

import com.slg.game.base.player.entity.PlayerEntity;
import com.slg.game.base.player.model.Player;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/2/12
 */
@Component
public class PlayerBaseService {

    /**
     * 修改玩家名字
     * @param player
     * @param name
     */
    public void changeName(Player player, String name) {

        // 判断消耗
        // ...

        // 修改名字
        player.getPlayerEntity().setName(name);
        // 入库
        player.getPlayerEntity().saveField(PlayerEntity.Fields.name);
        // 同步所有从实体
        player.getPlayerEntity().sync(PlayerEntity.Fields.name);
        // 短时间内再同步一次，会2秒后才进行同步
        player.getPlayerEntity().sync(PlayerEntity.Fields.name);
        // 全同步 和字段同步相互独立 且是立即同步
        player.getPlayerEntity().syncAll();

    }

}
