package com.slg.entity.mongo.repository;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.db.repository.BaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.util.List;

/**
 * MongoDB 仓储实现
 * 提供基于 MongoDB 的通用 CRUD 操作
 * 通过 @EnableMongo 自动引入，不参与组件扫描
 * 
 * @author yangxunan
 * @date 2025-12-18
 */
public class BaseMongoRepository implements BaseRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 插入单个文档
     *
     * @param entity 要插入的实体
     * @param <T> 实体类型
     * @return 插入后的实体（包含生成的ID）
     */
    @Override
    public <T> T insert(T entity) {
        try {
            return mongoTemplate.insert(entity);
        } catch (Exception e) {
            LoggerUtil.error("Failed to insert entity: {}", entity.getClass().getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 批量插入多个文档
     *
     * @param entities 要插入的实体列表
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 插入后的实体列表
     */
    @Override
    public <T> List<T> insertBatch(List<T> entities, Class<T> entityClass) {
        try {
            if (entities == null || entities.isEmpty()) {
                LoggerUtil.warn("批量插入时实体列表为空: {}", entityClass.getSimpleName());
                return entities;
            }
            return (List<T>) mongoTemplate.insert(entities, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("批量插入实体失败: {}, 数量: {}", entityClass.getSimpleName(), entities.size(), e);
            throw e;
        }
    }

    /**
     * 保存或更新文档
     * 如果文档存在（根据ID），则更新；否则插入
     *
     * @param entity 要保存的实体
     * @param <T> 实体类型
     * @return 保存后的实体
     */
    @Override
    public <T> T save(T entity) {
        try {
            return mongoTemplate.save(entity);
        } catch (Exception e) {
            LoggerUtil.error("Failed to save entity: {}", entity.getClass().getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 批量保存或更新文档
     * 对每个实体执行保存操作（存在则更新，不存在则插入）
     * 注意：此方法会逐个保存实体，性能不如 insertBatch，但可以处理更新场景
     *
     * @param entities 要保存的实体列表
     * @param <T> 实体类型
     * @return 保存后的实体列表
     */
    @Override
    public <T> List<T> saveBatch(List<T> entities) {
        try {
            if (entities == null || entities.isEmpty()) {
                LoggerUtil.warn("批量保存时实体列表为空");
                return entities;
            }
            
            // MongoDB 的 save 操作需要逐个处理，因为需要判断是插入还是更新
            entities.forEach(entity -> {
                try {
                    mongoTemplate.save(entity);
                } catch (Exception e) {
                    LoggerUtil.error("批量保存中单个实体失败: {}", entity.getClass().getSimpleName(), e);
                    throw e;
                }
            });
            
            return entities;
        } catch (Exception e) {
            LoggerUtil.error("批量保存实体失败", e);
            throw e;
        }
    }

    /**
     * 根据ID查找文档
     *
     * @param id 文档ID
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 找到的实体，未找到返回 null
     */
    @Override
    public <T> T findById(Object id, Class<T> entityClass) {
        try {
            return mongoTemplate.findById(id, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to find entity by id: {}, class: {}", id, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 查找指定类型的所有文档
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 所有文档列表
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        try {
            return mongoTemplate.findAll(entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to find all entities: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 根据字段值查找文档
     *
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 匹配的文档列表
     */
    public <T> List<T> findByField(String field, Object value, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where(field).is(value));
            return mongoTemplate.find(query, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to find entities by field: {}, value: {}, class: {}", 
                    field, value, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 根据字段值查找单个文档
     *
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 找到的实体，未找到返回 null
     */
    public <T> T findOneByField(String field, Object value, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where(field).is(value));
            return mongoTemplate.findOne(query, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to find one entity by field: {}, value: {}, class: {}", 
                    field, value, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 使用自定义查询条件查找文档
     *
     * @param query MongoDB 查询对象
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 匹配的文档列表
     */
    public <T> List<T> find(Query query, Class<T> entityClass) {
        try {
            return mongoTemplate.find(query, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to find entities with query, class: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 使用自定义查询条件查找单个文档
     *
     * @param query MongoDB 查询对象
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 找到的实体，未找到返回 null
     */
    public <T> T findOne(Query query, Class<T> entityClass) {
        try {
            return mongoTemplate.findOne(query, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to find one entity with query, class: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 根据ID更新单个字段
     *
     * @param id 文档ID
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 修改的文档数量
     */
    @Override
    public <T> long updateFieldById(Object id, String field, Object value, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update().set(field, value);
            return mongoTemplate.updateFirst(query, update, entityClass).getModifiedCount();
        } catch (Exception e) {
            LoggerUtil.error("更新字段失败: {}#{}, field={}", entityClass.getSimpleName(), id, field, e);
            throw e;
        }
    }

    /**
     * 批量更新实体的指定字段
     * 根据实体ID列表更新相同的字段值
     * 使用 $in 操作符一次性更新多个文档，性能优于逐个更新
     *
     * @param ids 实体ID列表
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 修改的文档数量
     */
    @Override
    public <T> long updateFieldByIds(List<Object> ids, String field, Object value, Class<T> entityClass) {
        try {
            if (ids == null || ids.isEmpty()) {
                LoggerUtil.warn("批量更新时ID列表为空: {}", entityClass.getSimpleName());
                return 0;
            }
            
            Query query = new Query(Criteria.where("_id").in(ids));
            Update update = new Update().set(field, value);
            long modifiedCount = mongoTemplate.updateMulti(query, update, entityClass).getModifiedCount();
            
            LoggerUtil.debug("批量更新字段成功: {}, field={}, IDs数量={}, 实际修改数量={}", 
                    entityClass.getSimpleName(), field, ids.size(), modifiedCount);
            return modifiedCount;
        } catch (Exception e) {
            LoggerUtil.error("批量更新字段失败: {}, field={}, IDs数量={}", 
                    entityClass.getSimpleName(), field, ids.size(), e);
            throw e;
        }
    }

    /**
     * 批量更新实体（根据查询条件和更新操作）
     * 提供更灵活的批量更新能力，支持复杂的更新逻辑
     *
     * @param ids 实体ID列表
     * @param update 更新操作
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 修改的文档数量
     */
    public <T> long updateByIds(List<Object> ids, Update update, Class<T> entityClass) {
        try {
            if (ids == null || ids.isEmpty()) {
                LoggerUtil.warn("批量更新时ID列表为空: {}", entityClass.getSimpleName());
                return 0;
            }
            
            Query query = new Query(Criteria.where("_id").in(ids));
            long modifiedCount = mongoTemplate.updateMulti(query, update, entityClass).getModifiedCount();
            
            LoggerUtil.debug("批量更新实体成功: {}, IDs数量={}, 实际修改数量={}", 
                    entityClass.getSimpleName(), ids.size(), modifiedCount);
            return modifiedCount;
        } catch (Exception e) {
            LoggerUtil.error("批量更新实体失败: {}, IDs数量={}", 
                    entityClass.getSimpleName(), ids.size(), e);
            throw e;
        }
    }

    /**
     * 根据ID更新单个文档
     *
     * @param id 文档ID
     * @param update 更新操作
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 修改的文档数量
     */
    public <T> long updateById(Object id, Update update, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where("_id").is(id));
            return mongoTemplate.updateFirst(query, update, entityClass).getModifiedCount();
        } catch (Exception e) {
            LoggerUtil.error("Failed to update entity by id: {}, class: {}", id, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 根据字段值更新文档
     *
     * @param field 字段名
     * @param value 字段值
     * @param update 更新操作
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 修改的文档数量
     */
    public <T> long updateByField(String field, Object value, Update update, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where(field).is(value));
            return mongoTemplate.updateMulti(query, update, entityClass).getModifiedCount();
        } catch (Exception e) {
            LoggerUtil.error("Failed to update entities by field: {}, value: {}, class: {}", 
                    field, value, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 使用自定义查询条件更新文档
     *
     * @param query MongoDB 查询对象
     * @param update 更新操作
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 修改的文档数量
     */
    public <T> long update(Query query, Update update, Class<T> entityClass) {
        try {
            return mongoTemplate.updateMulti(query, update, entityClass).getModifiedCount();
        } catch (Exception e) {
            LoggerUtil.error("Failed to update entities with query, class: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 根据ID删除文档
     *
     * @param id 文档ID
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 删除的文档数量
     */
    @Override
    public <T> long deleteById(Object id, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where("_id").is(id));
            return mongoTemplate.remove(query, entityClass).getDeletedCount();
        } catch (Exception e) {
            LoggerUtil.error("Failed to delete entity by id: {}, class: {}", id, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 根据字段值删除文档
     *
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 删除的文档数量
     */
    public <T> long deleteByField(String field, Object value, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where(field).is(value));
            return mongoTemplate.remove(query, entityClass).getDeletedCount();
        } catch (Exception e) {
            LoggerUtil.error("Failed to delete entities by field: {}, value: {}, class: {}", 
                    field, value, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 使用自定义查询条件删除文档
     *
     * @param query MongoDB 查询对象
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 删除的文档数量
     */
    public <T> long delete(Query query, Class<T> entityClass) {
        try {
            return mongoTemplate.remove(query, entityClass).getDeletedCount();
        } catch (Exception e) {
            LoggerUtil.error("Failed to delete entities with query, class: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 根据字段值统计文档数量
     *
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 文档数量
     */
    public <T> long countByField(String field, Object value, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where(field).is(value));
            return mongoTemplate.count(query, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to count entities by field: {}, value: {}, class: {}", 
                    field, value, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 统计所有文档数量
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 文档数量
     */
    public <T> long count(Class<T> entityClass) {
        try {
            return mongoTemplate.count(new Query(), entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to count all entities, class: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 使用自定义查询条件统计文档数量
     *
     * @param query MongoDB 查询对象
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 文档数量
     */
    public <T> long count(Query query, Class<T> entityClass) {
        try {
            return mongoTemplate.count(query, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to count entities with query, class: {}", entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 检查文档是否存在（根据ID）
     *
     * @param id 文档ID
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 存在返回 true
     */
    public <T> boolean existsById(Object id, Class<T> entityClass) {
        try {
            Query query = new Query(Criteria.where("_id").is(id));
            return mongoTemplate.exists(query, entityClass);
        } catch (Exception e) {
            LoggerUtil.error("Failed to check existence by id: {}, class: {}", id, entityClass.getSimpleName(), e);
            throw e;
        }
    }

    /**
     * 获取 MongoTemplate 实例用于高级操作
     *
     * @return MongoTemplate 实例
     */
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}

