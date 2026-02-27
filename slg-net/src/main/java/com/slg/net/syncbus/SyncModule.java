package com.slg.net.syncbus;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步模块枚举
 * 使用显式 ID 标识不同的同步实体对，确保新增枚举值不会影响已有协议
 *
 * @author yangxunan
 * @date 2026/02/12
 */
public enum SyncModule {

    /** 玩家同步：PlayerEntity(Game) -> ScenePlayerEntity(Scene) */
    PLAYER(1),
    ;

    private final int id;

    SyncModule(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** id -> 枚举的快速查找表 */
    private static final Map<Integer, SyncModule> ID_MAP = new HashMap<>();

    static {
        for (SyncModule m : values()) {
            if (ID_MAP.put(m.id, m) != null) {
                throw new IllegalStateException("SyncModule id 重复: " + m.id);
            }
        }
    }

    /**
     * 根据 id 查找对应的 SyncModule
     *
     * @param id 模块 ID
     * @return 对应的枚举值，不存在时返回 null
     */
    public static SyncModule fromId(int id) {
        return ID_MAP.get(id);
    }
}
