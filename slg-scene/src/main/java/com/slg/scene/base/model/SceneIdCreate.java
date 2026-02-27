package com.slg.scene.base.model;

import com.slg.scene.base.manager.SceneIdGeneratorManager;
import lombok.Getter;

/**
 * ID 生成器类型枚举
 * 定义不同业务的 ID 生成器，支持序列化和非序列化两种模式
 *
 * @author yangxunan
 * @date 2026/02/02
 */
@Getter
public enum SceneIdCreate {

    /**
     * 场景对象 ID（非序列化）
     * 场景对象通常不需要持久化，重启后重新分配即可
     */
    NODE("场景对象ID", false, 0),

    /**
     * 场景实体 创建可销毁场景时使用 如副本场景
     */
    SCENE("场景实体", false, 0),

    ;
    /**
     * 类型描述
     */
    private final String desc;

    /**
     * 是否需要序列化（持久化）
     */
    private final boolean serializable;

    /**
     * 持久化步长
     * 每次从数据库分配的 ID 数量，只对序列化类型有效
     */
    private final int step;

    SceneIdCreate(String desc, boolean serializable, int step) {
        this.desc = desc;
        this.serializable = serializable;
        this.step = step;
    }

    public long nextId(){
        return SceneIdGeneratorManager.getInstance().nextId(this);
    }
}
