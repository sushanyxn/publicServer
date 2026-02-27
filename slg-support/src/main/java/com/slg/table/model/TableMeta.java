package com.slg.table.model;

import com.slg.table.anno.Table;
import com.slg.table.anno.TableCompositeIndexField;
import com.slg.table.anno.TableId;
import com.slg.table.anno.TableIndex;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置表元数据
 * 通过反射扫描注解自动初始化
 *
 * @author yangxunan
 * @date 2025/12/26
 */
@Getter
@Setter
public class TableMeta<T> {

    /**
     * 配置表类
     */
    private Class<T> tableClass;
    
    /**
     * 表名（优先使用 @Table 的 alias，否则使用类名）
     */
    private String tableName;
    
    /**
     * 主键类型
     */
    private Class<?> idClass;
    
    /**
     * 主键字段
     */
    private Field idField;
    
    /**
     * 获取主键值的方法
     */
    private Method getIdMethod;
    
    /**
     * 索引字段映射：索引名 -> 字段
     */
    private Map<String, Field> indexFields;
    
    /**
     * 获取索引字段值的方法映射：索引名 -> getter方法
     */
    private Map<String, Method> indexGetterMethods;

    /**
     * 单字段索引唯一性标识：索引名 -> 是否唯一
     */
    private Map<String, Boolean> indexUniqueFlags;

    /**
     * 复合索引元数据：索引名 -> 复合索引元数据
     */
    private Map<String, CompositeIndexMeta> compositeIndexes;

    /**
     * 构造方法：通过反射扫描配置类的注解
     * 
     * @param configClass 配置类的 Class 对象
     */
    public TableMeta(Class<T> configClass) {
        this.tableClass = configClass;
        this.indexFields = new HashMap<>();
        this.indexGetterMethods = new HashMap<>();
        this.indexUniqueFlags = new HashMap<>();
        this.compositeIndexes = new HashMap<>();
        
        // 1. 扫描类上的 @Table 注解
        scanTableAnnotation();
        
        // 2. 扫描字段上的 @TableId 和 @TableIndex 注解
        scanFieldAnnotations();
        
        // 3. 扫描复合索引字段
        scanCompositeIndexFields();
        
        // 4. 验证必要字段是否存在
        validateMetadata();
    }

    /**
     * 扫描类上的 @Table 注解
     */
    private void scanTableAnnotation() {
        if (tableClass.isAnnotationPresent(Table.class)) {
            Table table = tableClass.getAnnotation(Table.class);
            String alias = table.alias();
            
            // 如果有别名，使用别名，否则使用类的简单名称
            this.tableName = (alias != null && !alias.isEmpty()) 
                ? alias 
                : tableClass.getSimpleName();
        } else {
            // 没有 @Table 注解，直接使用类名
            this.tableName = tableClass.getSimpleName();
        }
    }

    /**
     * 扫描字段上的注解
     */
    private void scanFieldAnnotations() {
        // 获取所有字段（包括父类字段）
        Field[] fields = tableClass.getDeclaredFields();
        
        for (Field field : fields) {
            // 扫描 @TableId 注解
            if (field.isAnnotationPresent(TableId.class)) {
                processTableIdField(field);
            }
            
            // 扫描 @TableIndex 注解
            if (field.isAnnotationPresent(TableIndex.class)) {
                processTableIndexField(field);
            }
        }
    }

    /**
     * 处理 @TableId 字段
     */
    private void processTableIdField(Field field) {
        if (this.idField != null) {
            throw new IllegalStateException(
                String.format("配置表 %s 存在多个 @TableId 字段: %s 和 %s", 
                    tableClass.getSimpleName(), idField.getName(), field.getName())
            );
        }
        
        this.idField = field;
        this.idClass = field.getType();
        this.getIdMethod = findGetterMethod(field);
    }

    /**
     * 处理 @TableIndex 字段
     */
    private void processTableIndexField(Field field) {
        TableIndex indexAnno = field.getAnnotation(TableIndex.class);
        String indexName = indexAnno.value();
        boolean unique = indexAnno.unique();
        
        if (indexName == null || indexName.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("配置表 %s 字段 %s 的 @TableIndex 注解缺少索引名称", 
                    tableClass.getSimpleName(), field.getName())
            );
        }
        
        // 检查索引名是否重复
        if (indexFields.containsKey(indexName)) {
            throw new IllegalStateException(
                String.format("配置表 %s 存在重复的索引名: %s", 
                    tableClass.getSimpleName(), indexName)
            );
        }
        
        indexFields.put(indexName, field);
        indexGetterMethods.put(indexName, findGetterMethod(field));
        indexUniqueFlags.put(indexName, unique);
        
    }

    /**
     * 查找字段对应的 getter 方法
     * 支持：getXxx() 或 isXxx()（布尔类型）
     */
    private Method findGetterMethod(Field field) {
        String fieldName = field.getName();
        String capitalizedName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        Method getterMethod = null;
        try {
            // 尝试 getXxx() 方法
            String getterName = "get" + capitalizedName;
            getterMethod = tableClass.getMethod(getterName);
        } catch (NoSuchMethodException e1) {
            try {
                // 对于 boolean 类型，尝试 isXxx() 方法
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    String isGetterName = "is" + capitalizedName;
                    getterMethod = tableClass.getMethod(isGetterName);
                }
            } catch (NoSuchMethodException e2) {
                //
            }
        }
        if (getterMethod == null) {
            throw new IllegalStateException(String.format("配置表 %s 的字段 %s 未找到可用的getter方法",
                    tableClass.getSimpleName(), fieldName));
        }
        return getterMethod;
    }

    /**
     * 验证元数据的完整性
     */
    private void validateMetadata() {
        if (idField == null) {
            throw new IllegalStateException(
                String.format("配置表 %s 缺少 @TableId 注解标注的主键字段", 
                    tableClass.getSimpleName())
            );
        }
    }

    /**
     * 获取配置数据的主键值
     */
    public Object getId(Object row) throws InvocationTargetException, IllegalAccessException {
        if (getIdMethod != null) {
            return getIdMethod.invoke(row);
        } else {
            // 如果没有 getter 方法，直接访问字段
            idField.setAccessible(true);
            return idField.get(row);
        }
    }

    /**
     * 获取配置数据的索引字段值
     * 
     * @param row 配置数据对象
     * @param indexName 索引名称
     * @return 索引字段的值
     */
    public Object getIndexValue(Object row, String indexName) throws InvocationTargetException, IllegalAccessException {
        Method getterMethod = indexGetterMethods.get(indexName);
        Field indexField = indexFields.get(indexName);
        
        if (indexField == null) {
            throw new IllegalArgumentException(
                String.format("配置表 %s 不存在索引: %s", tableClass.getSimpleName(), indexName)
            );
        }
        
        if (getterMethod != null) {
            return getterMethod.invoke(row);
        } else {
            // 如果没有 getter 方法，直接访问字段
            indexField.setAccessible(true);
            return indexField.get(row);
        }
    }

    /**
     * 扫描复合索引字段
     */
    private void scanCompositeIndexFields() {
        Field[] fields = tableClass.getDeclaredFields();
        
        // 临时存储：索引名 -> (order -> 字段)
        Map<String, Map<Integer, Field>> tempIndexes = new HashMap<>();
        Map<String, Boolean> uniqueFlags = new HashMap<>();
        
        // 第一遍：收集所有复合索引字段
        for (Field field : fields) {
            TableCompositeIndexField[] annotations = 
                field.getAnnotationsByType(TableCompositeIndexField.class);
            
            for (TableCompositeIndexField annotation : annotations) {
                String indexName = annotation.name();
                int order = annotation.order();
                boolean unique = annotation.unique();
                
                // 验证索引名
                if (indexName == null || indexName.isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("配置表 %s 字段 %s 的复合索引名称不能为空",
                            tableClass.getSimpleName(), field.getName()));
                }
                
                // 验证 order
                if (order < 0) {
                    throw new IllegalArgumentException(
                        String.format("配置表 %s 字段 %s 的复合索引顺序不能小于 0",
                            tableClass.getSimpleName(), field.getName()));
                }
                
                // 收集字段
                Map<Integer, Field> orderMap = 
                    tempIndexes.computeIfAbsent(indexName, k -> new HashMap<>());
                
                if (orderMap.containsKey(order)) {
                    throw new IllegalStateException(
                        String.format("配置表 %s 复合索引 %s 存在重复的 order: %d",
                            tableClass.getSimpleName(), indexName, order));
                }
                
                orderMap.put(order, field);
                
                // 记录唯一性标记
                if (unique) {
                    uniqueFlags.put(indexName, true);
                }
            }
        }
        
        // 第二遍：按顺序构建复合索引元数据
        for (Map.Entry<String, Map<Integer, Field>> entry : tempIndexes.entrySet()) {
            String indexName = entry.getKey();
            Map<Integer, Field> orderMap = entry.getValue();
            
            // 验证顺序连续性
            validateOrderContinuity(indexName, orderMap);
            
            // 创建复合索引元数据
            boolean unique = uniqueFlags.getOrDefault(indexName, false);
            CompositeIndexMeta meta = new CompositeIndexMeta(indexName, unique);
            
            // 按 order 排序添加字段
            List<Integer> orders = new ArrayList<>(orderMap.keySet());
            orders.sort(Integer::compareTo);
            
            for (Integer order : orders) {
                Field field = orderMap.get(order);
                Method getter = findGetterMethod(field);
                meta.addField(field, getter);
            }
            
            compositeIndexes.put(indexName, meta);
        }
    }
    
    /**
     * 验证 order 的连续性
     */
    private void validateOrderContinuity(String indexName, Map<Integer, Field> orderMap) {
        List<Integer> orders = new ArrayList<>(orderMap.keySet());
        orders.sort(Integer::compareTo);
        
        if (orders.isEmpty()) {
            return;
        }
        
        // 验证从 0 开始
        if (orders.get(0) != 0) {
            throw new IllegalStateException(
                String.format("配置表 %s 复合索引 %s 的 order 必须从 0 开始",
                    tableClass.getSimpleName(), indexName));
        }
        
        // 验证连续性
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i) != i) {
                throw new IllegalStateException(
                    String.format("配置表 %s 复合索引 %s 的 order 不连续，缺少 order=%d",
                        tableClass.getSimpleName(), indexName, i));
            }
        }
    }
    
    /**
     * 获取复合索引的值
     * 
     * @param row 配置数据对象
     * @param indexName 复合索引名称
     * @return 复合索引键
     */
    public CompositeIndexKey getCompositeIndexValue(Object row, String indexName) 
            throws InvocationTargetException, IllegalAccessException {
        CompositeIndexMeta meta = compositeIndexes.get(indexName);
        if (meta == null) {
            throw new IllegalArgumentException(
                String.format("配置表 %s 不存在复合索引: %s",
                    tableClass.getSimpleName(), indexName));
        }
        
        List<Method> getters = meta.getGetterMethods();
        Object[] values = new Object[getters.size()];
        
        for (int i = 0; i < getters.size(); i++) {
            values[i] = getters.get(i).invoke(row);
        }
        
        return new CompositeIndexKey(values);
    }

}
