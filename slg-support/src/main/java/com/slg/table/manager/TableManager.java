package com.slg.table.manager;

import com.slg.table.config.TableProperties;
import com.slg.table.model.*;
import com.slg.table.util.TableLoadUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置表管理器
 * 负责注册、加载和管理所有配置表
 *
 * @author yangxunan
 * @date 2025/12/26
 */
@Component
public class TableManager {

    @Autowired
    private TableProperties tableProperties;

    /**
     * 配置表缓存：配置类 -> 配置表实例
     */
    @Getter
    private final Map<Class<?>, AbstractTable<?>> tableMap = new HashMap<>();

    /**
     * 注册配置表
     * 根据配置类的主键类型自动选择合适的 Table 实现
     *
     * @param configClass 配置类
     * @param <T> 配置类型
     * @return 配置表实例
     */
    public <T> AbstractTable<T> registerTable(Class<T> configClass) {
        // 检查是否已注册
        @SuppressWarnings("unchecked")
        AbstractTable<T> existTable = (AbstractTable<T>) tableMap.get(configClass);
        if (existTable != null) {
            return existTable;
        }

        // 创建 TableMeta
        AbstractTable<T> table = getAbstractTable(configClass);
        tableMap.put(configClass, table);

        // 加载 CSV 文件
        loadCSVFile(table);

        return table;
    }

    public  <T> AbstractTable<T> getAbstractTable(Class<T> configClass){
        TableMeta<T> tableMeta = new TableMeta<T>(configClass);

        // 根据主键类型选择 Table 实现
        AbstractTable<T> table;
        if (tableMeta.getIdClass() == int.class || tableMeta.getIdClass() == Integer.class) {
            table = new TableInt<>();
        } else if (tableMeta.getIdClass() == long.class || tableMeta.getIdClass() == Long.class) {
            table = new TableLong<>();
        } else {
            table = new TableCommon<>();
        }

        table.setTableMeta(tableMeta);
        return table;
    }

    /**
     * 获取配置表实例
     *
     * @param configClass 配置类
     * @param <T> 配置类型
     * @return 配置表实例，如果未注册则返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> AbstractTable<T> getTable(Class<T> configClass) {
        return (AbstractTable<T>) tableMap.get(configClass);
    }

    /**
     * 加载 CSV 配置文件
     *
     * @param table 配置表缓存
     */
    public <T> void loadCSVFile(AbstractTable<T> table) {
        String fileName = table.getTableMeta().getTableName();
        String fullPath = getFullFilePath(fileName);
        
        TableLoadUtil.loadCSVFile(table, fullPath);
    }

    /**
     * 获取配置表目录的完整路径
     *
     * @return 配置表目录路径
     */
    public String getTablePath() {
        String configPath = tableProperties.getPath();
        
        // 如果是绝对路径，直接返回
        Path path = Paths.get(configPath);
        if (path.isAbsolute()) {
            return configPath;
        }
        
        // 如果是相对路径，相对于项目根目录
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, configPath).toString();
    }

    /**
     * 获取配置文件的完整路径
     *
     * @param fileName 文件名（不含扩展名）
     * @return 完整文件路径
     */
    public String getFullFilePath(String fileName) {
        return Paths.get(getTablePath(), fileName + ".csv").toString();
    }

}

