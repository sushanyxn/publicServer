package com.slg.game.base.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.db.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Game 服 ID 生成器持久化实体
 * 用于序列化类型的 ID 生成器持久化最大序列号
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "game_id_generator")
@CacheConfig(maxSize = -1, expireMinutes = -1, writeDelay = false)
@FieldNameConstants
public class GameIdGeneratorEntity extends BaseEntity<String> {

    /** ID 生成器类型代码 */
    private String typeCode;

    /** 服务器 ID */
    private int serverId;

    /** 当前最大序列号 */
    private long maxSequence;

    /**
     * 初始化
     *
     * @param name     ID 生成器类型名
     * @param serverId 服务器 ID
     */
    public void init(String name, int serverId) {
        this.id = generateId(name, serverId);
        this.typeCode = name;
        this.serverId = serverId;
        this.maxSequence = 0L;
    }

    /**
     * 生成实体 ID
     *
     * @param name     ID 生成器类型名
     * @param serverId 服务器 ID
     * @return 实体 ID
     */
    public static String generateId(String name, int serverId) {
        return serverId + "_" + name;
    }

    @Override
    public void save() {
        // 由 GameIdGeneratorManager 管理保存
    }

    @Override
    public void saveField(String fieldName) {
        // 由 GameIdGeneratorManager 管理保存
    }
}
