package com.slg.entity.db.persist;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.entity.db.repository.BaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步持久化服务
 * 所有数据库操作都通过 {@link Executor#Persistence} 在虚拟线程中异步执行
 * 相同实体ID的操作按顺序执行，不同实体ID的操作并发执行
 *
 * @author yangxunan
 * @date 2025-12-22
 */
@Component
public class AsyncPersistenceService {

    @Autowired
    private BaseRepository repository;

    /**
     * 异步保存实体
     * 使用持久化模块执行器串行队列，相同ID的操作按顺序执行
     *
     * @param entity 要保存的实体（必须继承 BaseEntity）
     * @param <T>    实体类型
     */
    public <T extends BaseEntity<?>> void save(T entity) {
        if (entity == null || entity.getId() == null) {
            LoggerUtil.error("实体或实体ID为空，无法保存");
            return;
        }

        Object id = entity.getId();
        Executor.Persistence.execute(id, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> repository.save(entity), id);
        });
    }

    /**
     * 异步插入实体
     *
     * @param entity 要插入的实体（必须继承 BaseEntity）
     * @param <T>    实体类型
     */
    public <T extends BaseEntity<?>> void insert(T entity) {
        if (entity == null || entity.getId() == null) {
            LoggerUtil.error("实体或实体ID为空，无法插入");
            return;
        }

        Object id = entity.getId();
        Executor.Persistence.execute(id, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> repository.insert(entity), id);
        });
    }

    /**
     * 异步批量插入实体
     * 批量插入使用通用key，避免与单个实体操作冲突
     *
     * @param entities    要插入的实体列表
     * @param entityClass 实体类
     * @param <T>         实体类型
     */
    public <T extends BaseEntity<?>> void insertBatch(List<T> entities, Class<T> entityClass) {
        if (entities == null || entities.isEmpty()) {
            LoggerUtil.warn("实体列表为空，无法批量插入: {}", entityClass.getSimpleName());
            return;
        }

        String batchKey = "insertBatch:" + entityClass.getName();
        Executor.Persistence.execute(batchKey, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> {
                repository.insertBatch(entities, entityClass);
                LoggerUtil.debug("批量插入实体成功: {}, 数量={}", entityClass.getSimpleName(), entities.size());
            }, batchKey);
        });
    }

    /**
     * 异步批量保存实体
     *
     * @param entities 要保存的实体列表
     * @param <T>      实体类型
     */
    public <T extends BaseEntity<?>> void saveBatch(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            LoggerUtil.warn("实体列表为空，无法批量保存");
            return;
        }

        Class<?> entityClass = entities.get(0).getClass();
        String batchKey = "saveBatch:" + entityClass.getName();
        Executor.Persistence.execute(batchKey, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> repository.saveBatch(entities), batchKey);
        });
    }

    /**
     * 异步查找实体
     *
     * @param id          实体ID
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return CompletableFuture，包含实体对象
     */
    public <T extends BaseEntity<?>> CompletableFuture<T> findById(Object id, Class<T> entityClass) {
        if (id == null) {
            LoggerUtil.error("实体ID为空，无法查询: {}", entityClass.getSimpleName());
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<T> future = new CompletableFuture<>();

        Executor.Persistence.execute(id, () -> {
            try {
                T result = repository.findById(id, entityClass);
                future.complete(result);
            } catch (Exception e) {
                LoggerUtil.error("查询实体失败: {}#{}", entityClass.getSimpleName(), id, e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * 异步查找所有实体
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return CompletableFuture，包含所有实体列表
     */
    public <T extends BaseEntity<?>> CompletableFuture<List<T>> findAll(Class<T> entityClass) {
        if (entityClass == null) {
            LoggerUtil.error("实体类为空，无法查询");
            return CompletableFuture.completedFuture(List.of());
        }

        CompletableFuture<List<T>> future = new CompletableFuture<>();

        String queryKey = "findAll:" + entityClass.getName();
        Executor.Persistence.execute(queryKey, () -> {
            try {
                List<T> result = repository.findAll(entityClass);
                future.complete(result);
            } catch (Exception e) {
                LoggerUtil.error("查询所有实体失败: {}", entityClass.getSimpleName(), e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * 异步根据字段值查找实体
     *
     * @param field       字段名
     * @param value       字段值
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return CompletableFuture，包含匹配的实体列表
     */
    public <T extends BaseEntity<?>> CompletableFuture<List<T>> findByField(
            String field, Object value, Class<T> entityClass) {
        if (field == null || entityClass == null) {
            LoggerUtil.error("字段或实体类为空，无法查询");
            return CompletableFuture.completedFuture(List.of());
        }

        CompletableFuture<List<T>> future = new CompletableFuture<>();

        String queryKey = "findByField:" + entityClass.getName() + ":" + field;
        Executor.Persistence.execute(queryKey, () -> {
            try {
                List<T> result = repository.findByField(field, value, entityClass);
                future.complete(result);
            } catch (Exception e) {
                LoggerUtil.error("根据字段查询实体失败: {}, field={}, value={}",
                        entityClass.getSimpleName(), field, value, e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * 异步更新单个字段
     *
     * @param id          实体ID
     * @param field       字段名
     * @param value       字段值
     * @param entityClass 实体类
     * @param <T>         实体类型
     */
    public <T extends BaseEntity<?>> void updateField(Object id, String field, Object value, Class<T> entityClass) {
        if (id == null || field == null || entityClass == null) {
            LoggerUtil.error("参数为空，无法更新字段: {}", entityClass == null ? "unknown" : entityClass.getSimpleName());
            return;
        }

        Executor.Persistence.execute(id, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> {
                long count = repository.updateFieldById(id, field, value, entityClass);
                if (count == 0) {
                    LoggerUtil.warn("更新字段未找到目标文档: {}#{}, field={}",
                            entityClass.getSimpleName(), id, field);
                }
            }, id);
        });
    }

    /**
     * 异步批量更新指定字段
     *
     * @param ids         实体ID列表
     * @param field       字段名
     * @param value       字段值
     * @param entityClass 实体类
     * @param <T>         实体类型
     */
    public <T extends BaseEntity<?>> void updateFieldBatch(List<Object> ids, String field, Object value, Class<T> entityClass) {
        if (ids == null || ids.isEmpty() || field == null || entityClass == null) {
            LoggerUtil.warn("参数为空，无法批量更新字段: {}", entityClass == null ? "unknown" : entityClass.getSimpleName());
            return;
        }

        String batchKey = "updateFieldBatch:" + entityClass.getName() + ":" + field;
        Executor.Persistence.execute(batchKey, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> {
                long count = repository.updateFieldByIds(ids, field, value, entityClass);
                LoggerUtil.debug("批量更新字段成功: {}, field={}, IDs数量={}, 实际更新={}",
                        entityClass.getSimpleName(), field, ids.size(), count);
            }, batchKey);
        });
    }

    /**
     * 异步软删除实体
     * 标记 deleted=true，不从数据库中真正移除
     *
     * @param id          实体ID
     * @param entityClass 实体类
     * @param <T>         实体类型
     */
    public <T extends BaseEntity<?>> void deleteById(Object id, Class<T> entityClass) {
        if (id == null) {
            LoggerUtil.error("实体ID为空，无法删除: {}", entityClass.getSimpleName());
            return;
        }

        Executor.Persistence.execute(id, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> repository.deleteById(id, entityClass), id);
        });
    }

    /**
     * 异步真实删除实体
     * 从数据库中永久移除记录
     *
     * @param id          实体ID
     * @param entityClass 实体类
     * @param <T>         实体类型
     */
    public <T extends BaseEntity<?>> void hardDeleteById(Object id, Class<T> entityClass) {
        if (id == null) {
            LoggerUtil.error("实体ID为空，无法真实删除: {}", entityClass.getSimpleName());
            return;
        }

        Executor.Persistence.execute(id, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> repository.hardDeleteById(id, entityClass), id);
        });
    }

    /**
     * 异步批量清理所有已软删除的实体
     * 从数据库中永久移除所有 deleted=true 的记录
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     */
    public <T extends BaseEntity<?>> void purgeDeleted(Class<T> entityClass) {
        if (entityClass == null) {
            LoggerUtil.error("实体类为空，无法清理软删除数据");
            return;
        }

        String purgeKey = "purgeDeleted:" + entityClass.getName();
        Executor.Persistence.execute(purgeKey, () -> {
            PersistenceRetryWrapper.executeWithRetry(() -> {
                long count = repository.hardDeleteAllDeleted(entityClass);
                LoggerUtil.debug("清理软删除实体完成: {}, 删除数量={}", entityClass.getSimpleName(), count);
            }, purgeKey);
        });
    }
}
