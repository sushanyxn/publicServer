package com.slg.entity.mysql.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.entity.mysql.repository.BaseMysqlRepository;
import com.slg.entity.mysql.util.MysqlConnectionValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MySQL 数据库配置类
 * 通过 {@code @EnableMysql} 注解自动引入
 *
 * <p>配置内容：
 * <ul>
 *   <li>启用 JPA 审计功能（{@code @CreatedDate}、{@code @LastModifiedDate}）</li>
 *   <li>启用事务管理</li>
 *   <li>导入 MySQL 连接验证器和仓储实现</li>
 *   <li>Spring Boot 自动配置处理 DataSource / EntityManagerFactory / TransactionManager</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/24
 */
@EnableJpaAuditing
@EnableTransactionManagement
@Import({
    MysqlConnectionValidator.class,
    BaseMysqlRepository.class
})
public class MysqlConfiguration {

    /**
     * 配置 Jackson ObjectMapper
     * 使用 {@code @Primary} 确保整个应用统一使用 JsonUtil 的配置
     *
     * @return JsonUtil 的 ObjectMapper 实例
     */
    @Bean
    @Primary
    public ObjectMapper mysqlObjectMapper() {
        ObjectMapper mapper = JsonUtil.getMapper();
        LoggerUtil.debug("MySQL 使用 JsonUtil 的 ObjectMapper 进行序列化");
        return mapper;
    }
}
