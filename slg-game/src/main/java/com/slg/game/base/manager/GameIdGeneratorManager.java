package com.slg.game.base.manager;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.game.base.entity.GameIdGeneratorEntity;
import com.slg.game.base.model.GameIdCreate;
import com.slg.game.base.model.GameIdGenerator;
import com.slg.game.core.config.GameServerConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Game 服 ID 生成器管理器
 * 管理各类型 ID 生成器，负责初始化与序列号持久化
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Component
@Getter
public class GameIdGeneratorManager {

    @Getter
    private static GameIdGeneratorManager instance;

    @Autowired
    private GameServerConfiguration gameServerConfiguration;

    @EntityCacheInject
    private EntityCache<GameIdGeneratorEntity> idGeneratorEntityCache;

    private final Map<GameIdCreate, GameIdGenerator> generators = new EnumMap<>(GameIdCreate.class);

    @PostConstruct
    public void init() {
        instance = this;
        for (GameIdCreate type : GameIdCreate.values()) {
            initGenerator(type);
        }
    }

    private void initGenerator(GameIdCreate type) {
        int serverId = gameServerConfiguration.getServerId();
        long currentSequence;
        long maxSequence;

        if (type.isSerializable()) {
            GameIdGeneratorEntity entity = loadOrCreateEntity(type, serverId);
            long savedMaxSequence = entity.getMaxSequence();
            currentSequence = savedMaxSequence;
            maxSequence = savedMaxSequence;
        } else {
            currentSequence = 0L;
            maxSequence = -1L;
        }

        GameIdGenerator generator = new GameIdGenerator(type, serverId, currentSequence, maxSequence);
        generator.setManager(this);
        generators.put(type, generator);
    }

    private GameIdGeneratorEntity loadOrCreateEntity(GameIdCreate type, int serverId) {
        String id = GameIdGeneratorEntity.generateId(type.name(), serverId);
        GameIdGeneratorEntity entity = idGeneratorEntityCache.findById(id);
        if (entity == null) {
            entity = new GameIdGeneratorEntity();
            entity.init(type.name(), serverId);
            idGeneratorEntityCache.insert(entity);
        }
        return entity;
    }

    public GameIdGenerator getGenerator(GameIdCreate type) {
        GameIdGenerator generator = generators.get(type);
        if (generator == null) {
            throw new RuntimeException("ID生成器不存在: " + type.name());
        }
        return generator;
    }

    public long nextId(GameIdCreate type) {
        return getGenerator(type).nextId();
    }

    /**
     * 保存最大序列号（由 GameIdGenerator 在分配新段时调用）
     */
    public void saveMaxSequence(GameIdCreate type, long maxSequence) {
        if (!type.isSerializable()) {
            return;
        }
        int serverId = gameServerConfiguration.getServerId();
        String id = GameIdGeneratorEntity.generateId(type.name(), serverId);
        GameIdGeneratorEntity entity = idGeneratorEntityCache.findById(id);
        if (entity == null) {
            return;
        }
        entity.setMaxSequence(maxSequence);
        idGeneratorEntityCache.save(entity);
    }
}
