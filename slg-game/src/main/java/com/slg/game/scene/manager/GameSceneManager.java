package com.slg.game.scene.manager;

import com.slg.game.scene.table.SceneTable;
import com.slg.table.anno.Table;
import com.slg.table.model.TableInt;
import org.springframework.stereotype.Component;

/**
 * game的场景数据管理
 *
 * @author yangxunan
 * @date 2026/2/2
 */
@Component
public class GameSceneManager {

    /**
     * 场景配置表
     */
    @Table
    private TableInt<SceneTable> sceneTable;

    /**
     * 根据场景ID获取场景配置
     *
     * @param sceneId 场景ID
     * @return 场景配置
     */
    public SceneTable getSceneTable(int sceneId) {
        return sceneTable.get(sceneId);
    }
}
