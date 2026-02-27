package com.slg.entity.mysql.anno;

/**
 * 序列化格式枚举
 * 定义 {@link Serialized} 注解支持的两种存储格式
 *
 * @author yangxunan
 * @date 2026/02/24
 */
public enum SerializeFormat {

    /**
     * JSON 字符串格式，对应 VARCHAR/TEXT/JSON 列
     */
    JSON,

    /**
     * JSON byte[] 格式，对应 VARBINARY/BLOB 列
     */
    BYTES
}
