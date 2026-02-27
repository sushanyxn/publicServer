package com.slg.entity.mysql.anno;

import com.slg.entity.mysql.config.MysqlConfiguration;
import com.slg.entity.mysql.lifecycle.MysqlLifeCycleConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 MySQL 数据库
 * 在 Spring Boot 主类上添加此注解以启用 MySQL 数据库功能
 *
 * <p>使用此注解的模块必须在 pom.xml 中显式引入 JPA 和 MySQL 依赖：
 * <pre>
 * &lt;dependency&gt;
 *     &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
 *     &lt;artifactId&gt;spring-boot-starter-data-jpa&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 * &lt;dependency&gt;
 *     &lt;groupId&gt;com.mysql&lt;/groupId&gt;
 *     &lt;artifactId&gt;mysql-connector-j&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 * </pre>
 * 这些依赖在 slg-support 中声明为 optional，不会自动传递，
 * 未引入这些依赖的模块不会加载任何 MySQL 相关组件。
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableMysql
 * public class NewModuleMain {
 *     public static void main(String[] args) {
 *         SpringApplication.run(NewModuleMain.class, args);
 *     }
 * }
 * }
 * </pre>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * spring:
 *   datasource:
 *     url: jdbc:mysql://localhost:3306/slg_db?useUnicode=true&amp;characterEncoding=utf-8&amp;serverTimezone=Asia/Shanghai
 *     username: root
 *     password: xxx
 *     driver-class-name: com.mysql.cj.jdbc.Driver
 *   jpa:
 *     hibernate:
 *       ddl-auto: update
 *     show-sql: false
 *     properties:
 *       hibernate:
 *         format_sql: true
 *         dialect: org.hibernate.dialect.MySQLDialect
 * </pre>
 *
 * <p>注意：{@code @EnableMongo} 和 {@code @EnableMysql} 不可同时使用，同一进程只能启用其一
 *
 * @author yangxunan
 * @date 2026/02/24
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
    MysqlConfiguration.class,
    MysqlLifeCycleConfiguration.class
})
public @interface EnableMysql {
}
