package com.slg.entity.mysql.util;

import com.slg.common.log.LoggerUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * MySQL 连接验证工具
 * 用于测试和验证 MySQL 连接状态
 * 通过 {@code @EnableMysql} 自动引入，不参与组件扫描
 *
 * @author yangxunan
 * @date 2026/02/24
 */
public class MysqlConnectionValidator {

    @Autowired
    private DataSource dataSource;

    /**
     * 验证 MySQL 连接
     * 从连接池获取连接并验证其有效性
     *
     * @return true 如果连接成功
     */
    public boolean validateConnection() {
        try (Connection conn = dataSource.getConnection()) {
            LoggerUtil.debug("MySQL 连接验证成功！数据库: {}", conn.getCatalog());
            return conn.isValid(5);
        } catch (Exception e) {
            LoggerUtil.error("MySQL 连接验证失败！", e);
            LoggerUtil.error("  请检查：");
            LoggerUtil.error("    1. MySQL 服务是否启动？");
            LoggerUtil.error("    2. 连接配置是否正确？");
            LoggerUtil.error("    3. 网络是否可达？");
            return false;
        }
    }

    /**
     * 检查 MySQL 是否可用
     * 不抛出异常，只返回布尔值
     *
     * @return true 如果 MySQL 可用
     */
    public boolean isMysqlAvailable() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
}
