package com.slg.table.model;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * 基于 int 主键的配置表
 * 性能比hashmap快多了
 *
 * @author yangxunan
 * @date 2025/12/26
 */
public class TableInt<D> extends AbstractTable<D> {

    private Int2ObjectMap<D> datas = new Int2ObjectOpenHashMap<>();

    /**
     * 从配置文件写入数据
     *
     * @param d 配置数据
     */
    @Override
    public void writeData(D d) throws InvocationTargetException, IllegalAccessException {
        // 1. 存储主数据
        int id = (int) tableMeta.getId(d);
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
    public D get(int id){
        return datas.get(id);
    }

    /**
     * 获取所有配置
     *
     * @return 所有配置数据的集合
     */
    public Collection<D> getAll(){
        return datas.values();
    }

    /**
     * 替换数据（用于热更新）
     * 
     * @param newDatas 新的数据映射
     */
    public void setDatas(Int2ObjectMap<D> newDatas) {
        this.datas = newDatas;
    }

    /**
     * 获取数据映射（用于热更新）
     * 
     * @return 数据映射
     */
    public Int2ObjectMap<D> getDatas() {
        return datas;
    }

}
