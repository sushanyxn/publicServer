package com.slg.table.util;

import com.slg.common.log.LoggerUtil;
import com.slg.table.extend.TablePostProcessor;
import com.slg.table.model.AbstractTable;
import com.slg.table.model.TableMeta;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置表加载工具类（使用 Apache Commons CSV）
 * 负责 CSV 文件的读取、解析和数据加载
 *
 * @author yangxunan
 * @date 2025-12-29
 */
public class TableLoadUtil {

    /**
     * 加载 CSV 配置文件到配置表
     *
     * @param table 配置表实例
     * @param csvFilePath CSV 文件完整路径
     * @param <T> 配置类型
     */
    public static <T> void loadCSVFile(AbstractTable<T> table, String csvFilePath) {
        File file = new File(csvFilePath);
        if (!file.exists()) {
            throw new IllegalStateException(String.format("配置文件不存在: %s", csvFilePath));
        }

        String fileName = table.getTableMeta().getTableName();

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            // 使用 Commons CSV 解析，自动处理引号和转义
            CSVParser parser = CSVFormat.DEFAULT.parse(reader);
            List<CSVRecord> records = parser.getRecords();
            
            if (records.size() < 4) {
                throw new IllegalStateException(
                    String.format("配置文件 %s 格式错误，至少需要4行（字段名/类型/注释/数据）", fileName));
            }
            
            // 解析 CSV 头部（Luban 格式）
            CSVRecord headerRecord = records.get(0);
            // CSVRecord typeRecord = records.get(1);      // 预留：字段类型行
            // CSVRecord commentRecord = records.get(2);   // 预留：字段注释行
            
            String[] fieldNames = toStringArray(headerRecord);

            // 获取配置类的元数据
            TableMeta<T> tableMeta = table.getTableMeta();
            Class<T> configClass = tableMeta.getTableClass();
            
            // 构建字段名 -> Field 对象的映射
            Map<String, Field> fieldMap = buildFieldMap(configClass);
            
            // 从第4行开始解析数据
            int loadedCount = parseDataRows(table, records, fieldNames, fieldMap, fileName);
            
        } catch (Exception e) {
            LoggerUtil.error("配置表 {} 加载失败", fileName, e);
            throw new RuntimeException(String.format("配置表 %s 加载失败", fileName), e);
        }
    }

    /**
     * 将 CSVRecord 转换为 String 数组
     *
     * @param record CSV 记录
     * @return 字符串数组
     */
    private static String[] toStringArray(CSVRecord record) {
        String[] array = new String[record.size()];
        for (int i = 0; i < record.size(); i++) {
            array[i] = record.get(i);
        }
        return array;
    }

    /**
     * 构建字段映射
     *
     * @param configClass 配置类
     * @param <T> 配置类型
     * @return 字段名 -> Field 对象的映射
     */
    private static <T> Map<String, Field> buildFieldMap(Class<T> configClass) {
        Map<String, Field> fieldMap = new HashMap<>();
        
        for (Field field : configClass.getDeclaredFields()) {
            fieldMap.put(field.getName(), field);
            field.setAccessible(true); // 允许访问私有字段
        }
        
        return fieldMap;
    }

    /**
     * 解析数据行
     *
     * @param table 配置表
     * @param records CSV 所有记录
     * @param fieldNames 字段名数组
     * @param fieldMap 字段映射
     * @param fileName 文件名
     * @param <T> 配置类型
     * @return 成功加载的数据条数
     */
    private static <T> int parseDataRows(
            AbstractTable<T> table,
            List<CSVRecord> records,
            String[] fieldNames,
            Map<String, Field> fieldMap,
            String fileName) throws Exception {
        
        int loadedCount = 0;
        Class<T> configClass = table.getTableMeta().getTableClass();
        
        // 从第4行开始解析（跳过头部3行）
        for (int i = 3; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            
            // 跳过空行
            if (isEmptyRecord(record)) {
                continue;
            }
            
            // 验证列数
            if (record.size() != fieldNames.length) {
                LoggerUtil.warn("配置表 {} 第 {} 行数据列数不匹配，跳过。期望: {}, 实际: {}", 
                    fileName, i + 1, fieldNames.length, record.size());
                continue;
            }
            
            try {
                // 创建配置对象实例
                T configObj = configClass.getDeclaredConstructor().newInstance();
                
                // 设置字段值
                setFieldValues(configObj, fieldNames, record, fieldMap, fileName);

                if (configObj instanceof TablePostProcessor postProcessor){
                    postProcessor.postProcessAfterInitialization();
                }

                // 写入配置表
                table.writeData(configObj);

                loadedCount++;
            } catch (Exception e) {
                LoggerUtil.error("配置表 {} 第 {} 行数据解析失败", fileName, i + 1, e);
                throw new RuntimeException(
                    String.format("配置表 %s 第 %d 行解析失败", fileName, i + 1), e);
            }
        }
        
        return loadedCount;
    }

    /**
     * 判断是否为空行
     *
     * @param record CSV 记录
     * @return 如果是空行返回 true
     */
    private static boolean isEmptyRecord(CSVRecord record) {
        for (String value : record) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 设置对象的所有字段值
     *
     * @param configObj 配置对象
     * @param fieldNames 字段名数组
     * @param record CSV 记录
     * @param fieldMap 字段映射
     * @param fileName 文件名（用于日志）
     */
    private static void setFieldValues(
            Object configObj,
            String[] fieldNames,
            CSVRecord record,
            Map<String, Field> fieldMap,
            String fileName) throws IllegalAccessException {
        
        for (int j = 0; j < fieldNames.length; j++) {
            String fieldName = fieldNames[j].trim();
            String value = record.get(j).trim();
            
            Field field = fieldMap.get(fieldName);
            if (field == null) {
                continue;
            }
            
            // 类型转换并设置值
            setFieldValue(field, configObj, value);
        }
    }

    /**
     * 设置字段值（支持自动类型转换）
     * 
     * 支持的类型：
     * - 基本类型及其包装类：int, long, double, float, boolean, byte, short, char
     * - String
     * - 数组：格式为 "元素1,元素2,元素3"
     * - List：格式为 "元素1,元素2,元素3"
     * - Map：格式为 "key1:value1,key2:value2"
     * - 自定义对象（预留）
     *
     * @param field 字段对象
     * @param obj 对象实例
     * @param value 字符串值
     * @throws IllegalAccessException 反射访问异常
     */
    private static void setFieldValue(Field field, Object obj, String value) throws IllegalAccessException {
        TableTypeConverter.convertAndSet(field, obj, value);
    }

}

