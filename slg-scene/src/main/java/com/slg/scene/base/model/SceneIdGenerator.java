package com.slg.scene.base.model;

import com.slg.common.log.LoggerUtil;
import com.slg.scene.base.manager.SceneIdGeneratorManager;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID 生成器
 * 基于服务器 ID 和自增序列生成全局唯一 ID
 * 
 * ID 结构（64位）：
 * - 高 24 位：服务器 ID
 * - 低 40 位：序列号
 *
 * @author yangxunan
 * @date 2026/02/02
 */
@Getter
@Setter
public class SceneIdGenerator {

    /**
     * ID 生成器类型
     */
    private final SceneIdCreate type;

    /**
     * 服务器 ID
     */
    private final int serverId;

    /**
     * 服务器 ID 左移位数
     */
    private static final int SERVER_ID_SHIFT = 40;

    /**
     * 序列号掩码（低 40 位全为 1）
     */
    private static final long SEQUENCE_MASK = 0xFFFFFFFFFFL;

    /**
     * 服务器 ID 基础值（高位）
     * 在初始化时计算：serverId << 40
     */
    private final long serverIdBase;

    /**
     * 当前 ID（已包含服务器 ID 高位）
     */
    private final AtomicLong currentId;

    /**
     * 最大 ID（对于序列化类型，这是已分配的最大值，已包含服务器 ID 高位）
     */
    private final AtomicLong maxId;

    /**
     * ID 生成器管理器（用于序列化类型申请新的 ID 段）
     */
    private SceneIdGeneratorManager manager;

    /**
     * 构造函数
     *
     * @param type            ID 生成器类型
     * @param serverId        服务器 ID
     * @param currentSequence 当前序列号（不包含服务器 ID）
     * @param maxSequence     最大序列号（不包含服务器 ID）
     */
    public SceneIdGenerator(SceneIdCreate type, int serverId, long currentSequence, long maxSequence) {
        this.type = type;
        this.serverId = serverId;
        
        // 计算服务器 ID 基础值（只计算一次）
        this.serverIdBase = (long) serverId << SERVER_ID_SHIFT;
        
        // 将序列号加上服务器 ID 基础值，得到完整的 ID
        this.currentId = new AtomicLong(serverIdBase | currentSequence);
        
        if (type.isSerializable()) {
            // 序列化类型：最大 ID = 基础值 + 最大序列号
            this.maxId = new AtomicLong(serverIdBase | maxSequence);
        } else {
            // 非序列化类型：最大 ID = 基础值 + 序列号掩码（最大值）
            this.maxId = new AtomicLong(serverIdBase | SEQUENCE_MASK);
        }
        
        LoggerUtil.debug("ID生成器初始化: type={}, serverId={}, serverIdBase={}, currentId={}, maxId={}", 
                type.name(), serverId, serverIdBase, this.currentId.get(), this.maxId.get());
    }

    /**
     * 生成下一个 ID
     * 直接返回递增的 ID，不需要位运算组装
     *
     * @return 生成的 ID
     */
    public long nextId() {
        long id;
        
        if (type.isSerializable()) {
            // 序列化类型：需要检查是否超过已分配的段
            while (true) {
                id = currentId.incrementAndGet();
                
                // 检查是否超过最大 ID
                if (id > maxId.get()) {
                    synchronized (this) {
                        // 双重检查
                        if (currentId.get() > maxId.get()) {
                            // 分配新的 ID 段
                            allocateNewSegment();
                        }
                        // 分配完成后继续尝试
                        continue;
                    }
                }
                break;
            }
        } else {
            // 非序列化类型：直接递增
            id = currentId.incrementAndGet();
            
            // 检查是否溢出（超过最大 ID）
            if (id > maxId.get()) {
                LoggerUtil.error("ID生成器溢出: type={}, id={}, maxId={}", 
                        type.name(), id, maxId.get());
                throw new RuntimeException("ID生成器溢出: " + type.name());
            }
        }
        
        return id;
    }

    /**
     * 分配新的 ID 段（仅序列化类型）
     * 先持久化到数据库，再更新内存
     */
    private void allocateNewSegment() {
        if (manager == null) {
            throw new RuntimeException("ID生成器管理器未设置: " + type.name());
        }
        
        // 获取当前最大 ID 的序列号部分（去掉服务器 ID 高位）
        long oldMaxSequence = maxId.get() & SEQUENCE_MASK;
        long newMaxSequence = oldMaxSequence + type.getStep();
        
        LoggerUtil.debug("开始分配新的ID段: type={}, oldMaxSequence={}, newMaxSequence={}, step={}", 
                type.name(), oldMaxSequence, newMaxSequence, type.getStep());
        
        // 先持久化新的最大序列号到数据库（重要：必须先持久化）
        manager.saveMaxSequence(type, newMaxSequence);
        
        // 持久化成功后，更新内存中的最大 ID（基础值 + 新的最大序列号）
        long newMaxId = serverIdBase | newMaxSequence;
        maxId.set(newMaxId);
        
        LoggerUtil.debug("ID段分配完成: type={}, newMaxSequence={}, newMaxId={}", 
                type.name(), newMaxSequence, newMaxId);
    }
}
