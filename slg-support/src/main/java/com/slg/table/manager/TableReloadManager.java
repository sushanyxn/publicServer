package com.slg.table.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.table.anno.TableRefCheck;
import com.slg.table.model.AbstractTable;
import com.slg.table.model.TableCommon;
import com.slg.table.model.TableInt;
import com.slg.table.model.TableLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 配置表热更工具
 *
 * @author yangxunan
 * @date 2026/1/15
 */
@Component
public class TableReloadManager {

    @Autowired
    private TableManager tableManager;

    /**
     * 提交一组表名，这些表重新读取配置文件，生成配置对象AbstractTable
     * 在所有表都通过表检查后，将表的数据赋予TableManager中对应的AbstractTable
     * 
     * @param reloadTables 要重载的表名集合
     */
    public void reload(Collection<String> reloadTables) {
        LoggerUtil.debug("=== 开始配置表热更新 ===");
        LoggerUtil.debug("待重载表: {}", reloadTables);
        
        try {
            // ========== 阶段1：准备阶段 ==========
            // 1.1 建立表名到配置类的映射
            Map<String, Class<?>> tableNameToClass = buildTableNameIndex();
            
            // 1.2 验证表名是否存在
            validateTableNames(reloadTables, tableNameToClass);
            
            // 1.3 收集要重载的配置类
            Set<Class<?>> reloadClasses = collectReloadClasses(reloadTables, tableNameToClass);
            

            // ========== 阶段2：加载阶段 ==========
            // 2.1 创建临时表并加载新数据
            Map<Class<?>, AbstractTable<?>> tempTables = loadTempTables(reloadClasses);

            
            // ========== 阶段3：检查阶段 ==========
            // 完整的引用检查覆盖 3 种情况：
            //   1. 新表 -> 新表（同时重载的表之间）
            //   2. 新表 -> 旧表（重载的表引用未重载的表）
            //   3. 旧表 -> 新表（未重载的表引用重载的表）
            
            // 3.1 创建混合上下文（旧表+新表）
            Map<Class<?>, AbstractTable<?>> mergedContext = createMergedContext(tempTables);
            
            // 3.2 检查新表的引用完整性（覆盖情况 1 和 2）
            checkReloadedTables(reloadClasses, tempTables, mergedContext);
            
            // 3.3 检查依赖表（覆盖情况 3）
            checkDependentTables(reloadClasses, mergedContext);
            

            // ========== 阶段4：应用阶段 ==========
            // 4.1 原子性替换数据
            applyReloadedData(tempTables);
            
            // 4.2 触发热更新监听器
            fireReloadListeners(reloadClasses);
            
            LoggerUtil.debug("=== 配置表热更新成功，共重载 {} 个表 ===", reloadTables.size());
            
        } catch (Exception e) {
            LoggerUtil.error("配置表热更新失败", e);
        }
    }

    // ==================== 阶段1：准备阶段 ====================

    /**
     * 建立表名到配置类的映射
     */
    private Map<String, Class<?>> buildTableNameIndex() {
        Map<String, Class<?>> index = new HashMap<>();
        
        for (Map.Entry<Class<?>, AbstractTable<?>> entry : tableManager.getTableMap().entrySet()) {
            Class<?> configClass = entry.getKey();
            AbstractTable<?> table = entry.getValue();
            String tableName = table.getTableMeta().getTableName();
            
            index.put(tableName, configClass);
        }
        
        return index;
    }

    /**
     * 验证表名是否存在
     */
    private void validateTableNames(Collection<String> reloadTables, 
                                    Map<String, Class<?>> tableNameToClass) {
        List<String> notFoundTables = new ArrayList<>();
        
        for (String tableName : reloadTables) {
            if (!tableNameToClass.containsKey(tableName)) {
                notFoundTables.add(tableName);
            }
        }
        
        if (!notFoundTables.isEmpty()) {
            throw new IllegalArgumentException("未找到配置表: " + notFoundTables);
        }
    }

    /**
     * 收集要重载的配置类
     */
    private Set<Class<?>> collectReloadClasses(Collection<String> reloadTables,
                                               Map<String, Class<?>> tableNameToClass) {
        Set<Class<?>> reloadClasses = new HashSet<>();
        
        for (String tableName : reloadTables) {
            reloadClasses.add(tableNameToClass.get(tableName));
        }
        
        return reloadClasses;
    }

    // ==================== 阶段2：加载阶段 ====================

    /**
     * 创建临时表并加载新数据
     */
    private Map<Class<?>, AbstractTable<?>> loadTempTables(Set<Class<?>> reloadClasses) {
        Map<Class<?>, AbstractTable<?>> tempTables = new HashMap<>();
        List<String> loadErrors = new ArrayList<>();
        
        for (Class<?> configClass : reloadClasses) {
            try {
                // 创建新的 AbstractTable 实例（不注册到 TableManager）
                AbstractTable<?> newTable = tableManager.getAbstractTable(configClass);
                
                // 加载 CSV 文件（会自动构建索引）
                tableManager.loadCSVFile(newTable);
                
                // 存入临时表
                tempTables.put(configClass, newTable);
                
                String tableName = newTable.getTableMeta().getTableName();
                int dataCount = newTable.getAll().size();

            } catch (Exception e) {
                String tableName = getTableName(configClass);
                String errorMsg = String.format("表 %s 加载失败: %s", tableName, e.getMessage());
                loadErrors.add(errorMsg);
                LoggerUtil.error(errorMsg, e);
            }
        }
        
        // 如果任何表加载失败，中止流程
        if (!loadErrors.isEmpty()) {
            throw new RuntimeException("配置表加载失败:\n" + String.join("\n", loadErrors));
        }
        
        return tempTables;
    }

    /**
     * 获取表名（辅助方法）
     */
    private String getTableName(Class<?> configClass) {
        AbstractTable<?> table = tableManager.getTable(configClass);
        if (table != null) {
            return table.getTableMeta().getTableName();
        }
        return configClass.getSimpleName();
    }

    // ==================== 阶段3：检查阶段 ====================

    /**
     * 创建混合上下文（旧表+新表）
     */
    private Map<Class<?>, AbstractTable<?>> createMergedContext(
            Map<Class<?>, AbstractTable<?>> tempTables) {
        
        Map<Class<?>, AbstractTable<?>> mergedContext = new HashMap<>(tableManager.getTableMap());
        mergedContext.putAll(tempTables);  // 新表覆盖旧表
        
        return mergedContext;
    }

    /**
     * 检查新表的引用完整性
     * 检查范围：
     * - 新表 -> 新表（同时重载的表之间的引用）
     * - 新表 -> 旧表（重载的表引用未重载的表）
     */
    private void checkReloadedTables(Set<Class<?>> reloadClasses,
                                    Map<Class<?>, AbstractTable<?>> tempTables,
                                    Map<Class<?>, AbstractTable<?>> mergedContext) {
        List<String> checkErrors = new ArrayList<>();
        
        for (Class<?> configClass : reloadClasses) {
            AbstractTable<?> newTable = tempTables.get(configClass);
            String tableName = newTable.getTableMeta().getTableName();
            
            try {
                checkSingleTable(configClass, newTable, mergedContext);
            } catch (Exception e) {
                String errorMsg = String.format("表 %s 检查失败: %s", tableName, e.getMessage());
                checkErrors.add(errorMsg);
                LoggerUtil.error(errorMsg, e);
            }
        }
        
        if (!checkErrors.isEmpty()) {
            throw new RuntimeException("配置表检查失败:\n" + String.join("\n", checkErrors));
        }
    }

    /**
     * 检查依赖表（引用了新表的旧表）
     * 检查范围：
     * - 旧表 -> 新表（未重载的表引用重载的表）
     */
    private void checkDependentTables(Set<Class<?>> reloadClasses,
                                     Map<Class<?>, AbstractTable<?>> mergedContext) {
        // 查找所有依赖表
        Set<Class<?>> dependentClasses = findDependentTables(reloadClasses);
        
        if (dependentClasses.isEmpty()) {
            return;
        }
        
        List<String> checkErrors = new ArrayList<>();
        
        for (Class<?> dependentClass : dependentClasses) {
            AbstractTable<?> oldTable = tableManager.getTable(dependentClass);
            String tableName = oldTable.getTableMeta().getTableName();
            
            try {
                checkSingleTable(dependentClass, oldTable, mergedContext);
            } catch (Exception e) {
                String errorMsg = String.format("依赖表 %s 检查失败: %s", tableName, e.getMessage());
                checkErrors.add(errorMsg);
                LoggerUtil.error(errorMsg, e);
            }
        }
        
        if (!checkErrors.isEmpty()) {
            throw new RuntimeException("依赖表检查失败:\n" + String.join("\n", checkErrors));
        }
    }

    /**
     * 检查单个表的引用完整性
     */
    private void checkSingleTable(Class<?> configClass, 
                                 AbstractTable<?> table,
                                 Map<Class<?>, AbstractTable<?>> tableContext) {
        // 扫描所有带 @TableRefCheck 注解的字段
        Field[] fields = configClass.getDeclaredFields();
        
        for (Field field : fields) {
            if (!field.isAnnotationPresent(TableRefCheck.class)) {
                continue;
            }
            
            TableRefCheck refCheckAnno = field.getAnnotation(TableRefCheck.class);
            Class<?> refTableClass = refCheckAnno.value();
            
            // 从上下文中获取引用表
            AbstractTable<?> refTable = tableContext.get(refTableClass);
            if (refTable == null) {
                throw new IllegalStateException(
                    String.format("字段 %s 引用的表 %s 未加载", 
                        field.getName(), refTableClass.getSimpleName()));
            }
            
            // 验证引用完整性
            checkFieldReference(field, table, refTable);
        }
    }

    /**
     * 检查字段的引用完整性
     */
    private void checkFieldReference(Field field, 
                                    AbstractTable<?> sourceTable,
                                    AbstractTable<?> refTable) {
        field.setAccessible(true);
        
        String sourceTableName = sourceTable.getTableMeta().getTableName();
        String refTableName = refTable.getTableMeta().getTableName();
        
        // 收集所有引用的ID
        Set<Object> refIds = new HashSet<>();
        for (Object data : sourceTable.getAll()) {
            try {
                Object fieldValue = field.get(data);
                if (fieldValue != null) {
                    refIds.add(fieldValue);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("读取字段值失败: " + field.getName(), e);
            }
        }
        
        // 检查每个引用的ID是否存在
        List<Object> missingIds = new ArrayList<>();
        for (Object refId : refIds) {
            if (!existsInTable(refTable, refId)) {
                missingIds.add(refId);
            }
        }
        
        if (!missingIds.isEmpty()) {
            throw new IllegalStateException(
                String.format("表 %s 字段 %s 引用了不存在的 %s ID: %s",
                    sourceTableName, field.getName(), refTableName, missingIds));
        }
    }

    /**
     * 检查ID是否在表中存在
     */
    private boolean existsInTable(AbstractTable<?> table, Object id) {
        if (table instanceof TableInt) {
            return ((TableInt<?>) table).get((Integer) id) != null;
        } else if (table instanceof TableLong) {
            return ((TableLong<?>) table).get((Long) id) != null;
        } else if (table instanceof TableCommon) {
            return ((TableCommon<?>) table).get(id) != null;
        }
        return false;
    }

    /**
     * 查找依赖于指定表的其他表
     */
    private Set<Class<?>> findDependentTables(Set<Class<?>> reloadClasses) {
        Set<Class<?>> dependents = new HashSet<>();
        
        // 遍历所有已加载的表
        for (Class<?> tableClass : tableManager.getTableMap().keySet()) {
            // 跳过要重载的表
            if (reloadClasses.contains(tableClass)) {
                continue;
            }
            
            // 检查是否引用了要重载的表
            if (hasReferenceToTables(tableClass, reloadClasses)) {
                dependents.add(tableClass);
            }
        }
        
        return dependents;
    }

    /**
     * 检查表是否引用了指定的表集合
     */
    private boolean hasReferenceToTables(Class<?> tableClass, Set<Class<?>> targetClasses) {
        Field[] fields = tableClass.getDeclaredFields();
        
        for (Field field : fields) {
            if (!field.isAnnotationPresent(TableRefCheck.class)) {
                continue;
            }
            
            TableRefCheck refCheckAnno = field.getAnnotation(TableRefCheck.class);
            Class<?> refTableClass = refCheckAnno.value();
            
            if (targetClasses.contains(refTableClass)) {
                return true;
            }
        }
        
        return false;
    }

    // ==================== 阶段4：应用阶段 ====================

    /**
     * 原子性替换数据
     */
    private void applyReloadedData(Map<Class<?>, AbstractTable<?>> tempTables) {
        // 对每个要替换的表加锁
        for (Map.Entry<Class<?>, AbstractTable<?>> entry : tempTables.entrySet()) {
            Class<?> configClass = entry.getKey();
            AbstractTable<?> newTable = entry.getValue();
            AbstractTable<?> oldTable = tableManager.getTable(configClass);
            
            if (oldTable == null) {
                throw new IllegalStateException("旧表不存在: " + configClass.getSimpleName());
            }
            
            // 同步替换数据
            synchronized (oldTable) {
                replaceTableData(oldTable, newTable);
            }
            
            String tableName = oldTable.getTableMeta().getTableName();
            LoggerUtil.debug("表 {} 数据替换完成", tableName);
        }
    }

    /**
     * 替换表的内部数据
     * 
     * @param oldTable 原表（保持引用不变）
     * @param newTable 新表（数据源）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void replaceTableData(AbstractTable<?> oldTable, AbstractTable<?> newTable) {
        try {
            // 1. 替换主数据（根据类型处理）
            replaceMainData(oldTable, newTable);
            
            // 2. 替换单字段索引数据（直接赋值）
            AbstractTable rawOldTable = oldTable;
            AbstractTable rawNewTable = newTable;
            rawOldTable.setIndexdDatas(rawNewTable.getIndexdDatas());
            
            // 3. 替换复合索引数据（直接赋值）
            rawOldTable.setCompositeIndexDatas(rawNewTable.getCompositeIndexDatas());
            
        } catch (Exception e) {
            throw new RuntimeException("替换表数据失败", e);
        }
    }

    /**
     * 替换主数据（直接赋值，原子操作）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void replaceMainData(AbstractTable<?> oldTable, AbstractTable<?> newTable) {
        if (oldTable instanceof TableInt && newTable instanceof TableInt) {
            // Int2ObjectMap - 直接替换引用
            TableInt oldIntTable = (TableInt) oldTable;
            TableInt newIntTable = (TableInt) newTable;
            oldIntTable.setDatas(newIntTable.getDatas());
            
        } else if (oldTable instanceof TableLong && newTable instanceof TableLong) {
            // Long2ObjectMap - 直接替换引用
            TableLong oldLongTable = (TableLong) oldTable;
            TableLong newLongTable = (TableLong) newTable;
            oldLongTable.setDatas(newLongTable.getDatas());
            
        } else if (oldTable instanceof TableCommon && newTable instanceof TableCommon) {
            // Map<Object, D> - 直接替换引用
            TableCommon oldCommonTable = (TableCommon) oldTable;
            TableCommon newCommonTable = (TableCommon) newTable;
            oldCommonTable.setDatas(newCommonTable.getDatas());
            
        } else {
            throw new IllegalStateException("表类型不匹配: " + 
                oldTable.getClass().getSimpleName() + " vs " + newTable.getClass().getSimpleName());
        }
    }

    /**
     * 触发热更新监听器
     * 
     * @param reloadClasses 重载的配置类集合
     */
    private void fireReloadListeners(Set<Class<?>> reloadClasses) {
        LoggerUtil.debug("开始触发热更新监听器");
        
        for (Class<?> configClass : reloadClasses) {
            AbstractTable<?> table = tableManager.getTable(configClass);
            if (table == null) {
                continue;
            }
            
            int listenerCount = table.getReloadListenerCount();
            if (listenerCount > 0) {
                String tableName = table.getTableMeta().getTableName();
                LoggerUtil.debug("配置表 {} 有 {} 个监听器待触发", tableName, listenerCount);

                table.fireReloadListeners();
            }
        }
        
        LoggerUtil.debug("热更新监听器全部触发完成");
    }

}
