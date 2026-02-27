package com.slg.table.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置表属性配置类
 * 读取 application.yml 中的 table 配置
 *
 * @author yangxunan
 * @date 2025-12-29
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "table")
public class TableProperties {

    /**
     * 配置表文件所在目录
     * 可以是相对路径（相对于项目根目录）或绝对路径
     */
    private String path = "table";

    /**
     * TableBean 扫描的基础包路径
     * 如果不配置，会自动扫描 Spring 主类所在的包及其子包
     * 
     * <p>配置示例（application.yml）：
     * <pre>
     * table:
     *   scan-packages:
     *     - com.slg
     * </pre>
     */
    private List<String> scanPackages = new ArrayList<>();

}

