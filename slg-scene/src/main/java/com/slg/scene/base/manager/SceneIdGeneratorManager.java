package com.slg.scene.base.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.scene.base.entity.SceneIdGeneratorEntity;
import com.slg.scene.base.model.SceneIdGenerator;
import com.slg.scene.base.model.SceneIdCreate;
import com.slg.scene.core.config.SceneServerConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * ID 生成器管理器
 * 管理所有类型的 ID 生成器，负责初始化和序列化
 *
 * @author yangxunan
 * @date 2026/02/02
 */
@Component
public class SceneIdGeneratorManager {

    @Getter
    private static SceneIdGeneratorManager instance;

    @Autowired
    private SceneServerConfiguration sceneServerConfiguration;

    @EntityCacheInject
    private EntityCache<SceneIdGeneratorEntity> idGeneratorEntityCache;

    /**
     * ID 生成器映射
     * key: ID 生成器类型
     * value: ID 生成器
     */
    private final Map<SceneIdCreate, SceneIdGenerator> generators = new EnumMap<SceneIdCreate, SceneIdGenerator>(SceneIdCreate.class);

    @PostConstruct
    public void init() {
        instance = this;

        // 初始化所有类型的 ID 生成器
        for (SceneIdCreate type : SceneIdCreate.values()) {
            initGenerator(type);
        }
    }

    /**
     * 初始化指定类型的 ID 生成器
     *
     * @param type ID 生成器类型
     */
    private void initGenerator(SceneIdCreate type) {
        int serverId = sceneServerConfiguration.getServerId();
        long currentSequence;
        long maxSequence;
        
        if (type.isSerializable()) {
            // 序列化类型：从数据库加载已分配的最大序列号
            SceneIdGeneratorEntity entity = loadOrCreateEntity(type, serverId);
            long savedMaxSequence = entity.getMaxSequence();
            
            // currentSequence 从已分配的最大序列号开始
            currentSequence = savedMaxSequence;
            // maxSequence 就是数据库中保存的值，不再加步长
            maxSequence = savedMaxSequence;
            
        } else {
            // 非序列化类型：从 0 开始
            currentSequence = 0L;
            maxSequence = -1L; // -1 表示不限制
            
        }
        
        SceneIdGenerator generator = new SceneIdGenerator(type, serverId, currentSequence, maxSequence);
        generator.setManager(this);
        generators.put(type, generator);
    }

    /**
     * 加载或创建 ID 生成器实体
     *
     * @param type     ID 生成器类型
     * @param serverId 服务器 ID
     * @return ID 生成器实体
     */
    private SceneIdGeneratorEntity loadOrCreateEntity(SceneIdCreate type, int serverId) {
        String id = SceneIdGeneratorEntity.generateId(type.name(), serverId);
        SceneIdGeneratorEntity entity = idGeneratorEntityCache.findById(id);
        
        if (entity == null) {
            entity = new SceneIdGeneratorEntity();
            entity.init(type.name(), serverId);
            idGeneratorEntityCache.insert(entity);
        }
        
        return entity;
    }

    /**
     * 获取指定类型的 ID 生成器
     *
     * @param type ID 生成器类型
     * @return ID 生成器
     */
    public SceneIdGenerator getGenerator(SceneIdCreate type) {
        SceneIdGenerator generator = generators.get(type);
        if (generator == null) {
            throw new RuntimeException("ID生成器不存在: " + type.name());
        }
        return generator;
    }

    /**
     * 生成下一个 ID
     *
     * @param type ID 生成器类型
     * @return 生成的 ID
     */
    public long nextId(SceneIdCreate type) {
        return getGenerator(type).nextId();
    }

    /**
     * 保存最大序列号（由 IdGenerator 调用）
     *
     * @param type        ID 生成器类型
     * @param maxSequence 最大序列号
     */
    public void saveMaxSequence(SceneIdCreate type, long maxSequence) {
        if (!type.isSerializable()) {
            return;
        }
        
        int serverId = sceneServerConfiguration.getServerId();
        String id = SceneIdGeneratorEntity.generateId(type.name(), serverId);
        SceneIdGeneratorEntity entity = idGeneratorEntityCache.findById(id);
        
        if (entity == null) {
            LoggerUtil.error("ID生成器实体不存在: type={}, serverId={}", type.name(), serverId);
            return;
        }
        
        entity.setMaxSequence(maxSequence);
        // 立即保存到数据库（writeDelaySec=0 保证立即写入）
        idGeneratorEntityCache.save(entity);
        
    }
}
