package com.slg.entity.mysql.repository;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.entity.db.repository.BaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 仓储实现
 * 提供基于 JPA（EntityManager）的通用 CRUD 操作
 * 通过 {@code @EnableMysql} 自动引入，不参与组件扫描
 *
 * <p>与 {@code BaseMongoRepository} 的对应关系：
 * <ul>
 *   <li>{@code MongoTemplate.insert()} → {@code EntityManager.persist()}</li>
 *   <li>{@code MongoTemplate.save()} → {@code EntityManager.merge()}</li>
 *   <li>{@code MongoTemplate.findById()} → {@code EntityManager.find()}</li>
 *   <li>{@code MongoTemplate.find(Query)} → JPQL 查询</li>
 *   <li>{@code MongoTemplate.updateFirst(Query, Update)} → find + 反射设值 + merge</li>
 *   <li>{@code MongoTemplate.remove(Query)} → find + {@code EntityManager.remove()}</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/24
 */
public class BaseMysqlRepository implements BaseRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public <T> T insert(T entity) {
        try {
            entityManager.persist(entity);
            return entity;
        } catch (Exception e) {
            LoggerUtil.error("插入实体失败: {}", entity.getClass().getSimpleName(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public <T> List<T> insertBatch(List<T> entities, Class<T> entityClass) {
        try {
            if (entities == null || entities.isEmpty()) {
                LoggerUtil.warn("批量插入时实体列表为空: {}", entityClass.getSimpleName());
                return entities;
            }
            for (T entity : entities) {
                entityManager.persist(entity);
            }
            entityManager.flush();
            LoggerUtil.debug("批量插入实体成功: {}, 数量={}", entityClass.getSimpleName(), entities.size());
            return entities;
        } catch (Exception e) {
            LoggerUtil.error("批量插入实体失败: {}, 数量: {}", entityClass.getSimpleName(), entities.size(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public <T> T save(T entity) {
        try {
            return entityManager.merge(entity);
        } catch (Exception e) {
            LoggerUtil.error("保存实体失败: {}", entity.getClass().getSimpleName(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public <T> List<T> saveBatch(List<T> entities) {
        try {
            if (entities == null || entities.isEmpty()) {
                LoggerUtil.warn("批量保存时实体列表为空");
                return entities;
            }
            List<T> result = new ArrayList<>(entities.size());
            for (T entity : entities) {
                result.add(entityManager.merge(entity));
            }
            entityManager.flush();
            return result;
        } catch (Exception e) {
            LoggerUtil.error("批量保存实体失败", e);
            throw e;
        }
    }

    @Override
    public <T> T findById(Object id, Class<T> entityClass) {
        try {
            T entity = entityManager.find(entityClass, id);
            if (entity instanceof BaseEntity<?> base && base.isDeleted()) {
                return null;
            }
            return entity;
        } catch (Exception e) {
            LoggerUtil.error("根据ID查找实体失败: {}, id={}", entityClass.getSimpleName(), id, e);
            throw e;
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        try {
            String jpql = "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.deleted = false";
            return entityManager.createQuery(jpql, entityClass).getResultList();
        } catch (Exception e) {
            LoggerUtil.error("查找所有实体失败: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    @Override
    public <T> List<T> findByField(String field, Object value, Class<T> entityClass) {
        try {
            String jpql = "SELECT e FROM " + entityClass.getSimpleName()
                         + " e WHERE e." + field + " = :value AND e.deleted = false";
            return entityManager.createQuery(jpql, entityClass)
                    .setParameter("value", value)
                    .getResultList();
        } catch (Exception e) {
            LoggerUtil.error("根据字段查找实体失败: {}#{}, field={}", entityClass.getSimpleName(), value, field, e);
            throw e;
        }
    }

    /**
     * 根据ID更新单个字段
     * 使用 load-modify-merge 模式，确保 {@code @Convert} 转换器生效
     */
    @Override
    @Transactional
    public <T> long updateFieldById(Object id, String field, Object value, Class<T> entityClass) {
        try {
            T entity = entityManager.find(entityClass, id);
            if (entity == null) {
                return 0;
            }
            if (entity instanceof BaseEntity<?> base && base.isDeleted()) {
                return 0;
            }
            setFieldValueRecursive(entity, field, value);
            entityManager.merge(entity);
            return 1;
        } catch (Exception e) {
            LoggerUtil.error("更新字段失败: {}#{}, field={}", entityClass.getSimpleName(), id, field, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public <T> long updateFieldByIds(List<Object> ids, String field, Object value, Class<T> entityClass) {
        try {
            if (ids == null || ids.isEmpty()) {
                LoggerUtil.warn("批量更新时ID列表为空: {}", entityClass.getSimpleName());
                return 0;
            }
            long count = 0;
            for (Object id : ids) {
                T entity = entityManager.find(entityClass, id);
                if (entity == null) {
                    continue;
                }
                if (entity instanceof BaseEntity<?> base && base.isDeleted()) {
                    continue;
                }
                setFieldValueRecursive(entity, field, value);
                entityManager.merge(entity);
                count++;
            }
            entityManager.flush();
            LoggerUtil.debug("批量更新字段成功: {}, field={}, IDs数量={}, 实际修改数量={}",
                    entityClass.getSimpleName(), field, ids.size(), count);
            return count;
        } catch (Exception e) {
            LoggerUtil.error("批量更新字段失败: {}, field={}, IDs数量={}",
                    entityClass.getSimpleName(), field, ids.size(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public <T> long deleteById(Object id, Class<T> entityClass) {
        try {
            T entity = entityManager.find(entityClass, id);
            if (entity == null) {
                return 0;
            }
            if (entity instanceof BaseEntity<?> base) {
                base.setDeleted(true);
                base.setDeleteTime(LocalDateTime.now());
                entityManager.merge(entity);
                return 1;
            }
            entityManager.remove(entity);
            return 1;
        } catch (Exception e) {
            LoggerUtil.error("软删除实体失败: {}, id={}", entityClass.getSimpleName(), id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public <T> long hardDeleteById(Object id, Class<T> entityClass) {
        try {
            T entity = entityManager.find(entityClass, id);
            if (entity == null) {
                return 0;
            }
            entityManager.remove(entity);
            return 1;
        } catch (Exception e) {
            LoggerUtil.error("真实删除实体失败: {}, id={}", entityClass.getSimpleName(), id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public <T> long hardDeleteAllDeleted(Class<T> entityClass) {
        try {
            String jpql = "DELETE FROM " + entityClass.getSimpleName() + " e WHERE e.deleted = true";
            return entityManager.createQuery(jpql).executeUpdate();
        } catch (Exception e) {
            LoggerUtil.error("批量清理软删除实体失败: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 遍历类层次结构设置字段值
     * 支持设置父类中定义的字段（如 BaseEntity 中的 createTime、updateTime 等）
     */
    private void setFieldValueRecursive(Object obj, String fieldName, Object value) {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                LoggerUtil.error("设置字段值失败: field={}", fieldName, e);
                throw new RuntimeException("设置字段值失败: " + fieldName, e);
            }
        }
        LoggerUtil.error("未找到字段: {}, class={}", fieldName, obj.getClass().getSimpleName());
        throw new RuntimeException("未找到字段: " + fieldName);
    }
}
