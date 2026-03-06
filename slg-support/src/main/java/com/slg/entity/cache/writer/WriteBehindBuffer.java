package com.slg.entity.cache.writer;

import com.slg.common.executor.GlobalScheduler;
import com.slg.common.executor.TaskModule;
import com.slg.common.log.LoggerUtil;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.entity.db.persist.AsyncPersistenceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Write-Behind 缓冲区
 * 管理待写入的数据，支持字段级和实体级两种写入模式
 * <p>使用 {@link ReentrantLock} 替代 synchronized，避免虚拟线程 pin 载体线程
 *
 * @author yangxunan
 * @date 2025-12-25
 * @param <T> 实体类型
 */
public class WriteBehindBuffer<T extends BaseEntity<?>> {

    private final Class<T> entityClass;
    private final AsyncPersistenceService persistenceService;

    /**
     * 字段级更新缓冲区
     * Key: 实体ID, Value: Map<字段名, 字段值>
     */
    private final HashMap<Object, HashMap<String, Object>> fieldUpdates
            = new HashMap<>();

    /**
     * 实体级更新缓冲区（全量更新）
     */
    private final HashMap<Object, T> entityUpdates = new HashMap<>();

    /**
     * 缓冲区操作锁（替代 synchronized，避免虚拟线程 pin 载体线程）
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 批量写入间隔（毫秒）
     */
    private final long batchIntervalMs;

    /**
     * 批量写入数量
     */
    private int batchSaveSize;

    /**
     * 定时刷新任务的 Future
     */
    private ScheduledFuture<?> scheduledTask;

    public WriteBehindBuffer(Class<T> entityClass,
                             AsyncPersistenceService persistenceService,
                             long batchIntervalMs,
                             int batchSaveSize,
                             GlobalScheduler globalScheduler) {
        this.entityClass = entityClass;
        this.persistenceService = persistenceService;
        this.batchIntervalMs = batchIntervalMs;
        this.batchSaveSize = batchSaveSize;

        startBatchWriter(globalScheduler);
    }

    /**
     * 字段级写入
     */
    public void writeField(Object entityId, String fieldName, Object value) {
        lock.lock();
        try {
            // 如果已经标记为实体级更新，直接返回
            if (entityUpdates.containsKey(entityId)) {
                return;
            }

            // 加入字段级更新缓冲区
            fieldUpdates
                    .computeIfAbsent(entityId, k -> new HashMap<>())
                    .put(fieldName, value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 实体级写入
     */
    public void writeEntity(T entity) {
        lock.lock();
        try {
            // 标记为实体级更新（全量保存）
            entityUpdates.put(entity.getId(), entity);

            // 清除该实体的字段级更新（避免重复）
            fieldUpdates.remove(entity.getId());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 启动批量写入任务
     * 使用 GlobalScheduler 统一调度，不再创建独立的 ScheduledExecutorService
     */
    private void startBatchWriter(GlobalScheduler globalScheduler) {
        scheduledTask = globalScheduler.scheduleWithFixedDelay(
                TaskModule.PERSISTENCE, (long) entityClass.getName().hashCode(),
                () -> {
                    try {
                        flushBuffer();
                    } catch (Exception e) {
                        LoggerUtil.error("批量写入失败: {}", entityClass.getSimpleName(), e);
                    }
                },
                batchIntervalMs, batchIntervalMs, TimeUnit.MILLISECONDS
        );
    }

    /**
     * 批量刷新（同时处理字段级和实体级）
     */
    public void flushBuffer() {
        lock.lock();
        try {
            // 1. 先刷新字段级更新（更高效）
            flushFieldUpdates();

            // 2. 再刷新实体级更新
            flushEntityUpdates();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 刷新字段级更新（优化版：使用批量更新）
     * 策略：按字段分组，相同字段、相同值的实体批量更新
     */
    private void flushFieldUpdates() {
        if (fieldUpdates.isEmpty()) {
            return;
        }

        // 1. 复制缓冲区数据并清空
        Map<Object, Map<String, Object>> batch = new HashMap<>();
        fieldUpdates.forEach((id, fields) -> {
            if (!fields.isEmpty()) {
                batch.put(id, new HashMap<>(fields));
            }
        });
        batch.keySet().forEach(fieldUpdates::remove);

        if (batch.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        int totalFieldCount = 0;

        // 2. 按字段名分组，构建 Map<字段名, Map<值, List<ID>>>
        Map<String, Map<Object, List<Object>>> fieldValueGroups = new HashMap<>();

        for (Map.Entry<Object, Map<String, Object>> entry : batch.entrySet()) {
            Object id = entry.getKey();
            Map<String, Object> fields = entry.getValue();

            totalFieldCount += fields.size();

            for (Map.Entry<String, Object> field : fields.entrySet()) {
                String fieldName = field.getKey();
                Object fieldValue = field.getValue();

                fieldValueGroups
                        .computeIfAbsent(fieldName, k -> new HashMap<>())
                        .computeIfAbsent(fieldValue, k -> new ArrayList<>())
                        .add(id);
            }
        }

        // 3. 批量更新：相同字段、相同值的实体批量更新（分批处理）
        int batchUpdateCount = 0;
        for (Map.Entry<String, Map<Object, List<Object>>> fieldEntry : fieldValueGroups.entrySet()) {
            String fieldName = fieldEntry.getKey();
            Map<Object, List<Object>> valueGroups = fieldEntry.getValue();

            for (Map.Entry<Object, List<Object>> valueEntry : valueGroups.entrySet()) {
                Object value = valueEntry.getKey();
                List<Object> ids = valueEntry.getValue();

                if (ids.size() == 1) {
                    persistenceService.updateField(ids.getFirst(), fieldName, value, entityClass);
                } else if (ids.size() <= batchSaveSize) {
                    persistenceService.updateFieldBatch(ids, fieldName, value, entityClass);
                    batchUpdateCount++;
                } else {
                    int totalBatches = (int) Math.ceil((double) ids.size() / batchSaveSize);
                    for (int i = 0; i < totalBatches; i++) {
                        int fromIndex = i * batchSaveSize;
                        int toIndex = Math.min(fromIndex + batchSaveSize, ids.size());
                        List<Object> subList = ids.subList(fromIndex, toIndex);
                        persistenceService.updateFieldBatch(subList, fieldName, value, entityClass);
                        batchUpdateCount++;
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        LoggerUtil.debug("批量字段更新完成: {}, 实体数={}, 字段数={}, 批量操作数={}, 耗时={}ms",
                entityClass.getSimpleName(), batch.size(), totalFieldCount, batchUpdateCount, elapsed);
    }

    /**
     * 刷新实体级更新
     * <p>统一使用 {@code persistenceService.saveBatch()} 提交，确保所有实体写入
     * 都走 className 分链（batchKey），避免与单条 save 的 entityId 分链并行冲突。
     * <p>saveBatch 内部由 {@link com.slg.entity.db.persist.PersistenceRetryWrapper}
     * 处理 DB 异常与重试，此处只负责分批提交。
     */
    private void flushEntityUpdates() {
        if (entityUpdates.isEmpty()) {
            return;
        }

        // 1. 复制缓冲区数据并清空
        Map<Object, T> batch = new HashMap<>(entityUpdates);
        entityUpdates.keySet().removeAll(batch.keySet());

        if (batch.isEmpty()) {
            return;
        }

        // 2. 转换为 List 并按批量大小分批提交（统一走 saveBatch）
        List<T> entities = new ArrayList<>(batch.values());

        if (entities.size() <= batchSaveSize) {
            persistenceService.saveBatch(entities);
        } else {
            int totalBatches = (int) Math.ceil((double) entities.size() / batchSaveSize);
            for (int i = 0; i < totalBatches; i++) {
                int fromIndex = i * batchSaveSize;
                int toIndex = Math.min(fromIndex + batchSaveSize, entities.size());
                persistenceService.saveBatch(entities.subList(fromIndex, toIndex));
            }
            LoggerUtil.debug("实体分批提交完成: {}, 总数={}, 批次={}",
                    entityClass.getSimpleName(), entities.size(), totalBatches);
        }
    }

    /**
     * 取消指定实体的所有待写入操作
     * 用于实体被删除时，防止缓冲区中残留的 save/update 操作将已删除数据"复活"
     *
     * @param entityId 实体ID
     */
    public void cancelPendingWrites(Object entityId) {
        lock.lock();
        try {
            entityUpdates.remove(entityId);
            fieldUpdates.remove(entityId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取缓冲区状态
     */
    public String getStatus() {
        return String.format("WriteBehindBuffer[%s]: 字段级缓冲=%d, 实体级缓冲=%d",
                entityClass.getSimpleName(),
                fieldUpdates.size(),
                entityUpdates.size());
    }
}
