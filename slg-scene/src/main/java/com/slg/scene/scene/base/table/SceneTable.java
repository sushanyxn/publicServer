package com.slg.scene.scene.base.table;

import com.slg.common.constant.SceneType;
import com.slg.table.anno.Table;
import com.slg.table.anno.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景配置表
 * 配置游戏中的场景信息（内城、大地图等）
 * 
 * @author yangxunan
 * @date 2026/02/02
 */
@Table
@Getter
@Setter
public class SceneTable {

    /**
     * 场景ID
     */
    @TableId
    private int id;

    /**
     * 场景名称
     */
    private String name;

    /**
     * 场景类型
     */
    private SceneType type;
}
