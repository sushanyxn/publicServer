package com.slg.frameworktest;

import com.slg.sharedmodules.progress.manager.ProgressManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 仅 Redis / Redis Route 集成测试用入口，不启用 MySQL，避免 DataSource 依赖
 *
 * @author framework-test
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.slg.common",
    "com.slg.sharedmodules",
    "com.slg.redis",
    "com.slg.frameworktest"
}, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {
        ProgressManager.class,
        FrameworkTestApplication.class,
        FrameworkTestRedisRouteApplication.class
    }
))
public class FrameworkTestRedisOnlyApplication {
}
