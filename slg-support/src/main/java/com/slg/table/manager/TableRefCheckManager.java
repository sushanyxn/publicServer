package com.slg.table.manager;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.table.anno.TableRefCheck;
import com.slg.table.model.AbstractTable;
import com.slg.table.model.TableMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * 配置表检查管理器
 * 负责在所有表加载完成后进行表关联完整性检查
 * 实现 SmartLifecycle，在 GameServerLifeCycle 之前执行
 * 
 * @author yangxunan
 * @date 2026/01/15
 */
@Component
public class TableRefCheckManager implements SmartLifecycle {

    @Autowired
    private TableManager tableManager;

    /**
     * 检查失败的记录数
     */
    private int failedCount = 0;

    /**
     * 是否正在运行
     */
    private volatile boolean running = false;

    /**
     * SmartLifecycle 启动回调
     * 在 GameServerLifeCycle.start() 之前执行配置表检查
     */
    @Override
    public void start() {
        LoggerUtil.info("开始配置表关联性检查");

        try {
            tableCheck();
            running = true;
            LoggerUtil.info("配置表关联性检查通过");
        } catch (Exception e) {
            LoggerUtil.error("配置表关联性检查失败", e);
            throw e;
        }
    }

    /**
     * SmartLifecycle 停止回调
     */
    @Override
    public void stop() {
        running = false;
    }

    /**
     * 是否正在运行
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 生命周期阶段
     */
    @Override
    public int getPhase() {
        return LifecyclePhase.TABLE_CHECK;
    }

    /**
     * 是否自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 检查表关联性
     */
    public void tableCheck() {
        checkTables();
        
        if (failedCount > 0) {
            LoggerUtil.error("配置表关联检查完成，共发现 {} 个错误！", failedCount);
            throw new IllegalStateException(
                String.format("配置表关联检查失败，共 %d 个错误，请检查日志", failedCount));
        }
    }

    /**
     * 加载完所有表以后，进行配置检查工作
     * 检查所有标注了 @TableRefCheck 的字段，验证引用完整性
     */
    public void checkTables() {
        failedCount = 0;
        
        Map<Class<?>, AbstractTable<?>> allTables = tableManager.getTableMap();
        
        if (allTables.isEmpty()) {
            LoggerUtil.warn("没有加载任何配置表，跳过检查");
            return;
        }

        // 遍历所有配置表
        for (Map.Entry<Class<?>, AbstractTable<?>> entry : allTables.entrySet()) {
            Class<?> configClass = entry.getKey();
            AbstractTable<?> table = entry.getValue();
            
            // 检查该表的所有字段
            checkTable(configClass, table);
        }
    }

    /**
     * 检查单个配置表
     * 
     * @param configClass 配置类
     * @param table 配置表实例
     */
    private void checkTable(Class<?> configClass, AbstractTable<?> table) {
        TableMeta<?> tableMeta = table.getTableMeta();
        String tableName = tableMeta.getTableName();
        
        // 获取所有字段
        Field[] fields = configClass.getDeclaredFields();
        
        // 收集需要检查的字段
        List<Field> refCheckFields = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(TableRefCheck.class)) {
                refCheckFields.add(field);
            }
        }
        
        if (refCheckFields.isEmpty()) {
            return; // 该表没有需要检查的字段
        }
        

        // 获取表中所有数据
        Collection<?> allData = getAllData(table);
        if (allData.isEmpty()) {
            return;
        }
        
        // 检查每个需要验证的字段
        for (Field field : refCheckFields) {
            checkFieldReference(tableName, field, allData);
        }
    }

    /**
     * 检查字段的引用完整性
     * 
     * @param tableName 表名
     * @param field 字段
     * @param allData 表中所有数据
     */
    private void checkFieldReference(String tableName, Field field, Collection<?> allData) {
        TableRefCheck refCheckAnno = field.getAnnotation(TableRefCheck.class);
        Class<?> refTableClass = refCheckAnno.value();
        
        // 获取引用的配置表
        AbstractTable<?> refTable = tableManager.getTable(refTableClass);
        if (refTable == null) {
            LoggerUtil.error("配置表 {} 字段 {} 引用的表 {} 未加载", 
                tableName, field.getName(), refTableClass.getSimpleName());
            failedCount++;
            return;
        }
        
        String refTableName = refTable.getTableMeta().getTableName();
        field.setAccessible(true);
        
        // 收集所有引用的ID
        Set<Object> refIds = new HashSet<>();
        for (Object data : allData) {
            try {
                Object fieldValue = field.get(data);
                if (fieldValue != null) {
                    refIds.add(fieldValue);
                }
            } catch (IllegalAccessException e) {
                LoggerUtil.error("读取字段值失败: {}.{}", tableName, field.getName(), e);
            }
        }
        
        // 检查每个引用的ID是否在目标表中存在
        int errorCount = 0;
        for (Object refId : refIds) {
            if (!existsInTable(refTable, refId)) {
                LoggerUtil.error("配置表 {} 字段 {} 引用了不存在的 {} ID: {}", 
                    tableName, field.getName(), refTableName, refId);
                errorCount++;
                failedCount++;
            }
        }
        
        if (errorCount > 0) {
            LoggerUtil.error("配置表 {} 字段 {} -> {} 关联检查失败，共 {} 个错误",
                    tableName, field.getName(), refTableName, errorCount);
        }
    }

    /**
     * 获取配置表中的所有数据
     * 
     * @param table 配置表
     * @return 所有数据的集合
     */
    private Collection<?> getAllData(AbstractTable<?> table) {
        // 通过反射调用 getAll() 方法
        try {
            var method = table.getClass().getMethod("getAll");
            Object result = method.invoke(table);
            if (result instanceof Collection) {
                return (Collection<?>) result;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LoggerUtil.error("获取配置表数据失败", e);
        }
        return Collections.emptyList();
    }

    /**
     * 检查ID是否在配置表中存在
     * 
     * @param table 配置表
     * @param id 主键ID
     * @return 如果存在返回 true
     */
    private boolean existsInTable(AbstractTable<?> table, Object id) {
        try {
            // 尝试调用 get(id) 方法
            Class<?> idClass = id.getClass();
            
            // 处理基本类型
            if (idClass == Integer.class) {
                var method = table.getClass().getMethod("get", int.class);
                Object result = method.invoke(table, ((Integer) id).intValue());
                return result != null;
            } else if (idClass == Long.class) {
                var method = table.getClass().getMethod("get", long.class);
                Object result = method.invoke(table, ((Long) id).longValue());
                return result != null;
            } else {
                var method = table.getClass().getMethod("get", Object.class);
                Object result = method.invoke(table, id);
                return result != null;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LoggerUtil.error("检查ID存在性失败: {}", id, e);
            return false;
        }
    }

}
