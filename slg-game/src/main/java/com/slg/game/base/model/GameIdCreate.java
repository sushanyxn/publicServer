package com.slg.game.base.model;

import com.slg.game.base.manager.GameIdGeneratorManager;
import lombok.Getter;

/**
 * Game 服 ID 生成器类型枚举
 * 定义不同业务的 ID 生成器，支持序列化和非序列化两种模式
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Getter
public enum GameIdCreate {

    /**
     * 玩家角色 ID（序列化）
     * 创角时分配，需持久化避免重启后重复
     */
    PLAYER("玩家ID", true, 1000),

    ;

    /** 类型描述 */
    private final String desc;

    /** 是否需要序列化（持久化） */
    private final boolean serializable;

    /** 持久化步长：每次从数据库分配的 ID 数量，仅对序列化类型有效 */
    private final int step;

    GameIdCreate(String desc, boolean serializable, int step) {
        this.desc = desc;
        this.serializable = serializable;
        this.step = step;
    }

    /**
     * 生成下一个 ID
     *
     * @return 生成的 ID
     */
    public long nextId() {
        return GameIdGeneratorManager.getInstance().nextId(this);
    }
}
