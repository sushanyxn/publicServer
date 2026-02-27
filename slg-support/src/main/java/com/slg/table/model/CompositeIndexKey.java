package com.slg.table.model;

import java.util.Arrays;

/**
 * 复合索引键
 * 用于组合多个字段值作为索引的键
 * 
 * @author yangxunan
 * @date 2026/01/14
 */
public class CompositeIndexKey {
    
    private final Object[] values;
    
    /**
     * 构造复合索引键
     * 
     * @param values 字段值数组，按顺序排列
     */
    public CompositeIndexKey(Object... values) {
        this.values = values;
    }
    
    /**
     * 获取所有值
     */
    public Object[] getValues() {
        return values;
    }
    
    /**
     * 获取指定位置的值
     */
    public Object getValue(int index) {
        return index >= 0 && index < values.length ? values[index] : null;
    }
    
    /**
     * 获取值的数量
     */
    public int size() {
        return values.length;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompositeIndexKey)) return false;
        CompositeIndexKey that = (CompositeIndexKey) o;
        return Arrays.equals(values, that.values);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
    
    @Override
    public String toString() {
        return "CompositeIndexKey" + Arrays.toString(values);
    }
}






