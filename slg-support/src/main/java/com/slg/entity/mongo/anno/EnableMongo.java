package com.slg.entity.mongo.anno;

import com.slg.entity.mongo.config.MongoDbConfiguration;
import com.slg.entity.mongo.lifecycle.MongoLifeCycleConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 MongoDB 数据库
 * 在 Spring Boot 主类上添加此注解以启用 MongoDB 数据库功能
 *
 * <p>使用此注解的模块必须在 pom.xml 中显式引入 MongoDB 依赖：
 * <pre>
 * &lt;dependency&gt;
 *     &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
 *     &lt;artifactId&gt;spring-boot-starter-data-mongodb&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 * </pre>
 * 该依赖在 slg-support 中声明为 optional，不会自动传递，
 * 未引入此依赖的模块不会加载任何 MongoDB 相关组件。
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableMongo
 * public class GameMain {
 *     public static void main(String[] args) {
 *         SpringApplication.run(GameMain.class, args);
 *     }
 * }
 * }
 * </pre>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * spring:
 *   data:
 *     mongodb:
 *       uri: mongodb://localhost:27017/slg_game
 *       auto-index-creation: true
 * </pre>
 *
 * @author yangxunan
 * @date 2026/02/24
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
    MongoDbConfiguration.class,
    MongoLifeCycleConfiguration.class
})
public @interface EnableMongo {
}
