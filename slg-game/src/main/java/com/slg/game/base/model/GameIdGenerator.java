package com.slg.game.base.model;

import com.slg.common.log.LoggerUtil;
import com.slg.game.base.manager.GameIdGeneratorManager;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Game 服 ID 生成器
 * 基于服务器 ID 和自增序列生成全局唯一 ID
 * <p>
 * ID 结构（64 位）：
 * - 高 24 位：服务器 ID
 * - 低 40 位：序列号
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Getter
@Setter
public class GameIdGenerator {

    private final GameIdCreate type;
    private final int serverId;

    private static final int SERVER_ID_SHIFT = 40;
    private static final long SEQUENCE_MASK = 0xFFFFFFFFFFL;

    private final long serverIdBase;
    private final AtomicLong currentId;
    private final AtomicLong maxId;

    private GameIdGeneratorManager manager;

    public GameIdGenerator(GameIdCreate type, int serverId, long currentSequence, long maxSequence) {
        this.type = type;
        this.serverId = serverId;
        this.serverIdBase = (long) serverId << SERVER_ID_SHIFT;
        this.currentId = new AtomicLong(serverIdBase | currentSequence);

        if (type.isSerializable()) {
            this.maxId = new AtomicLong(serverIdBase | maxSequence);
        } else {
            this.maxId = new AtomicLong(serverIdBase | SEQUENCE_MASK);
        }

        LoggerUtil.debug("ID生成器初始化: type={}, serverId={}, serverIdBase={}, currentId={}, maxId={}",
                type.name(), serverId, serverIdBase, this.currentId.get(), this.maxId.get());
    }

    public long nextId() {
        long id;

        if (type.isSerializable()) {
            while (true) {
                id = currentId.incrementAndGet();
                if (id > maxId.get()) {
                    synchronized (this) {
                        if (currentId.get() > maxId.get()) {
                            allocateNewSegment();
                        }
                        continue;
                    }
                }
                break;
            }
        } else {
            id = currentId.incrementAndGet();
            if (id > maxId.get()) {
                LoggerUtil.error("ID生成器溢出: type={}, id={}, maxId={}", type.name(), id, maxId.get());
                throw new RuntimeException("ID生成器溢出: " + type.name());
            }
        }

        return id;
    }

    private void allocateNewSegment() {
        if (manager == null) {
            throw new RuntimeException("ID生成器管理器未设置: " + type.name());
        }
        long oldMaxSequence = maxId.get() & SEQUENCE_MASK;
        long newMaxSequence = oldMaxSequence + type.getStep();
        LoggerUtil.debug("开始分配新的ID段: type={}, oldMaxSequence={}, newMaxSequence={}, step={}",
                type.name(), oldMaxSequence, newMaxSequence, type.getStep());

        manager.saveMaxSequence(type, newMaxSequence);
        long newMaxId = serverIdBase | newMaxSequence;
        maxId.set(newMaxId);
        LoggerUtil.debug("ID段分配完成: type={}, newMaxSequence={}, newMaxId={}", type.name(), newMaxSequence, newMaxId);
    }
}
