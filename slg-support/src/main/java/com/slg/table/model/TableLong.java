package com.slg.table.model;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * 基于 long 主键的配置表
 * 性能比hashmap快多了
 *
 * @author yangxunan
 * @date 2025-12-26
 */
public class TableLong<D> extends AbstractTable<D> {

    private Long2ObjectMap<D> datas = new Long2ObjectOpenHashMap<>();

    /**
     * 从配置文件写入数据
     *
     * @param d 配置数据
     */
    @Override
    public void writeData(D d) throws InvocationTargetException, IllegalAccessException {
        // 1. 存储主数据
        long id = (long) tableMeta.getId(d);
        datas.put(id, d);
        
        // 2. 构建所有索引
        buildIndexes(d);
    }

    /**
     * 获取单个配置（无装箱）
     *
     * @param id 配置ID
     * @return 配置数据，不存在返回 null
     */
    public D get(long id) {
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
    public void setDatas(Long2ObjectMap<D> newDatas) {
        this.datas = newDatas;
    }

    /**
     * 获取数据映射（用于热更新）
     * 
     * @return 数据映射
     */
    public Long2ObjectMap<D> getDatas() {
        return datas;
    }

}

