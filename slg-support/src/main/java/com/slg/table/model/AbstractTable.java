package com.slg.table.model;

import com.slg.common.log.LoggerUtil;
import com.slg.common.util.CollectionUtil;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * 配置表基类
 *
 * @author yangxunan
 * @date 2025/12/26
 */
@Getter
@Setter
public abstract class AbstractTable<D> {

    protected TableMeta<D> tableMeta;

    /**
     * 单字段索引数据：索引名 -> (索引值 -> 数据列表)
     */
    protected Map<String, Map<Object, List<D>>> indexdDatas = new HashMap<>();

    /**
     * 复合索引数据：索引名 -> (复合键 -> 数据列表)
     */
    protected Map<String, Map<CompositeIndexKey, List<D>>> compositeIndexDatas = new HashMap<>();

    /**
     * 热更新监听器列表
     */
    private final List<Runnable> reloadListeners = new ArrayList<>();

    public abstract void writeData(D d) throws InvocationTargetException, IllegalAccessException;

    /**
     * 获取所有配置数据
     * 
     * @return 所有配置数据的集合
     */
    public abstract Collection<D> getAll();

    /**
     * 构建索引数据（在 writeData 中调用）
     */
    protected void buildIndexes(D d) throws InvocationTargetException, IllegalAccessException {
        // 1. 构建单字段索引
        buildSingleFieldIndexes(d);
        
        // 2. 构建复合索引
        buildCompositeIndexes(d);
    }

    /**
     * 构建单字段索引
     */
    private void buildSingleFieldIndexes(D d) throws InvocationTargetException, IllegalAccessException {
        Map<String, Field> indexFields = tableMeta.getIndexFields();
        Map<String, Boolean> indexUniqueFlags = tableMeta.getIndexUniqueFlags();
        
        for (String indexName : indexFields.keySet()) {
            Object indexValue = tableMeta.getIndexValue(d, indexName);
            boolean unique = indexUniqueFlags.getOrDefault(indexName, false);
            
            Map<Object, List<D>> indexMap = 
                indexdDatas.computeIfAbsent(indexName, k -> new HashMap<>());
            
            // 如果是唯一索引，检查是否已存在
            if (unique && indexMap.containsKey(indexValue)) {
                String tableName = tableMeta.getTableName();
                throw new IllegalStateException(
                    String.format("配置表 %s 单字段索引 %s 违反唯一性约束：重复的索引值 %s",
                        tableName, indexName, indexValue));
            }
            
            List<D> list = indexMap.computeIfAbsent(indexValue, k -> new ArrayList<>());
            list.add(d);
        }
    }

    /**
     * 构建复合索引
     */
    private void buildCompositeIndexes(D d) throws InvocationTargetException, IllegalAccessException {
        Map<String, CompositeIndexMeta> compositeIndexes = tableMeta.getCompositeIndexes();
        
        for (Map.Entry<String, CompositeIndexMeta> entry : compositeIndexes.entrySet()) {
            String indexName = entry.getKey();
            CompositeIndexMeta meta = entry.getValue();
            
            CompositeIndexKey key = tableMeta.getCompositeIndexValue(d, indexName);
            
            Map<CompositeIndexKey, List<D>> indexMap = 
                compositeIndexDatas.computeIfAbsent(indexName, k -> new HashMap<>());
            
            // 如果是唯一索引，检查是否已存在
            if (meta.isUnique() && indexMap.containsKey(key)) {
                String tableName = tableMeta.getTableName();
                throw new IllegalStateException(
                    String.format("配置表 %s 复合索引 %s 违反唯一性约束：重复的索引键 %s",
                        tableName, indexName, key));
            }
            
            List<D> list = indexMap.computeIfAbsent(key, k -> new ArrayList<>());
            list.add(d);
        }
    }

    /**
     * 根据单字段索引获取配置
     * 
     * @param indexFlag 索引名称
     * @param index 索引值
     * @return 匹配的配置列表
     */
    public Collection<D> getIndexd(String indexFlag, Object index) {
        Map<Object, List<D>> map = indexdDatas.get(indexFlag);
        if (CollectionUtil.isBlank(map)) {
            return List.of();
        }
        List<D> indexData = map.get(index);
        if (CollectionUtil.isBlank(indexData)) {
            return List.of();
        }
        return indexData;
    }

    /**
     * 根据复合索引获取配置列表
     * 
     * @param indexName 复合索引名称
     * @param values 索引值数组，按 order 顺序传入
     * @return 匹配的配置列表
     */
    public Collection<D> getByCompositeIndex(String indexName, Object... values) {
        Map<CompositeIndexKey, List<D>> indexMap = compositeIndexDatas.get(indexName);
        if (CollectionUtil.isBlank(indexMap)) {
            return List.of();
        }
        
        CompositeIndexKey key = new CompositeIndexKey(values);
        List<D> result = indexMap.get(key);
        
        return result != null ? result : List.of();
    }

    /**
     * 根据复合索引获取单个配置（适用于唯一索引）
     * 
     * @param indexName 复合索引名称
     * @param values 索引值数组，按 order 顺序传入
     * @return 匹配的配置，不存在返回 null
     */
    public D getOneByCompositeIndex(String indexName, Object... values) {
        Collection<D> results = getByCompositeIndex(indexName, values);
        return results.isEmpty() ? null : results.iterator().next();
    }

    /**
     * 添加热更新监听器（编程式）
     * 
     * @param listener 监听器
     */
    public void addReloadListener(Runnable listener) {
        if (listener != null) {
            reloadListeners.add(listener);
        }
    }

    /**
     * 触发所有热更新监听器
     */
    public void fireReloadListeners() {
        String tableName = tableMeta != null ? tableMeta.getTableName() : "Unknown";
        
        if (reloadListeners.isEmpty()) {
            return;
        }

        for (int i = 0; i < reloadListeners.size(); i++) {
            try {
                reloadListeners.get(i).run();
            } catch (Exception e) {
                LoggerUtil.error("配置表 {} 的第 {} 个热更新监听器执行失败", tableName, i + 1, e);
            }
        }
    }

    /**
     * 获取热更新监听器数量
     * 
     * @return 监听器数量
     */
    public int getReloadListenerCount() {
        return reloadListeners.size();
    }
}
