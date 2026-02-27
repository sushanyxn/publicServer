package com.slg.table.model;

import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 复合索引元数据
 * 存储复合索引的字段信息
 * 
 * @author yangxunan
 * @date 2026/01/14
 */
@Getter
public class CompositeIndexMeta {
    
    /**
     * 索引名称
     */
    private final String indexName;
    
    /**
     * 是否唯一索引
     */
    private final boolean unique;
    
    /**
     * 字段列表（按 order 排序）
     */
    private final List<Field> fields;
    
    /**
     * Getter 方法列表（按 order 排序）
     */
    private final List<Method> getterMethods;
    
    public CompositeIndexMeta(String indexName, boolean unique) {
        this.indexName = indexName;
        this.unique = unique;
        this.fields = new ArrayList<>();
        this.getterMethods = new ArrayList<>();
    }
    
    /**
     * 添加字段
     */
    public void addField(Field field, Method getter) {
        fields.add(field);
        getterMethods.add(getter);
    }
    
    /**
     * 获取字段数量
     */
    public int getFieldCount() {
        return fields.size();
    }
}






