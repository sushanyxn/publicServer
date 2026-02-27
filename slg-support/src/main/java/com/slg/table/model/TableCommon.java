package com.slg.table.model;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于 Object 主键的通用配置表
 * 使用 HashMap 存储，适用于任意类型的 key
 *
 * @author yangxunan
 * @date 2025-12-26
 */
public class TableCommon<D> extends AbstractTable<D> {

    private Map<Object, D> datas = new HashMap<>();

    /**
     * 从配置文件写入数据
     *
     * @param d 配置数据
     */
    @Override
    public void writeData(D d) throws InvocationTargetException, IllegalAccessException {
        // 1. 存储主数据
        Object id = tableMeta.getId(d);
        datas.put(id, d);
        
        // 2. 构建所有索引
        buildIndexes(d);
    }

    /**
     * 获取单个配置
     *
     * @param id 配置ID
     * @return 配置数据，不存在返回 null
     */
    public D get(Object id) {
        return datas.get(id);
    }

    /**
     * 获取所有配置
     *
     * @return 所有配置数据的集合
     */
    public Collection<D> getAll() {
        return datas.values();
    }

    /**
     * 替换数据（用于热更新）
     * 
     * @param newDatas 新的数据映射
     */
    public void setDatas(Map<Object, D> newDatas) {
        this.datas = newDatas;
    }

    /**
     * 获取数据映射（用于热更新）
     * 
     * @return 数据映射
     */
    public Map<Object, D> getDatas() {
        return datas;
    }

}

