package com.slg.entity.db.repository;

import java.util.List;

/**
 * 基础数据仓储接口
 * 定义通用的数据访问操作，支持多种数据库实现（MongoDB、MySQL、Redis等）
 * 注意：此接口不应包含任何特定数据库的依赖
 * 
 * @author yangxunan
 * @date 2025-12-22
 */
public interface BaseRepository {

    /**
     * 插入实体
     *
     * @param entity 要插入的实体
     * @param <T> 实体类型
     * @return 插入后的实体（包含生成的ID）
     */
    <T> T insert(T entity);

    /**
     * 批量插入实体
     * 将多个实体一次性插入数据库，提高性能
     *
     * @param entities 要插入的实体列表
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 插入后的实体列表
     */
    <T> List<T> insertBatch(List<T> entities, Class<T> entityClass);

    /**
     * 保存实体
     * 如果实体已存在则更新，否则插入
     *
     * @param entity 要保存的实体
     * @param <T> 实体类型
     * @return 保存后的实体
     */
    <T> T save(T entity);

    /**
     * 批量保存实体
     * 对每个实体执行保存操作（存在则更新，不存在则插入）
     *
     * @param entities 要保存的实体列表
     * @param <T> 实体类型
     * @return 保存后的实体列表
     */
    <T> List<T> saveBatch(List<T> entities);

    /**
     * 根据ID查找实体
     *
     * @param id 实体ID
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 找到的实体，未找到返回 null
     */
    <T> T findById(Object id, Class<T> entityClass);

    /**
     * 查找指定类型的所有文档
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 所有文档列表
     */
    <T> List<T> findAll(Class<T> entityClass);

    /**
     * 根据字段值查找实体
     *
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 匹配的实体列表
     */
    <T> List<T> findByField(String field, Object value, Class<T> entityClass);

    /**
     * 根据ID更新单个字段
     *
     * @param id 文档ID
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 匹配到的文档数量（0 表示未找到目标文档）
     */
    <T> long updateFieldById(Object id, String field, Object value, Class<T> entityClass);

    /**
     * 批量更新实体的指定字段
     * 根据实体ID列表更新相同的字段值
     *
     * @param ids 实体ID列表
     * @param field 字段名
     * @param value 字段值
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 修改的文档数量
     */
    <T> long updateFieldByIds(List<Object> ids, String field, Object value, Class<T> entityClass);

    /**
     * 根据ID软删除实体
     * 标记 deleted=true，不真正从数据库中移除
     *
     * @param id 实体ID
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 受影响的数量
     */
    <T> long deleteById(Object id, Class<T> entityClass);

    /**
     * 根据ID真实删除实体
     * 从数据库中永久移除记录
     *
     * @param id 实体ID
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 删除的数量
     */
    <T> long hardDeleteById(Object id, Class<T> entityClass);

    /**
     * 批量真实删除所有已软删除的实体
     * 清理所有 deleted=true 的记录
     *
     * @param entityClass 实体类
     * @param <T> 实体类型
     * @return 删除的数量
     */
    <T> long hardDeleteAllDeleted(Class<T> entityClass);
}



