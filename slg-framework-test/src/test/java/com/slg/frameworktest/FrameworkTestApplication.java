package com.slg.frameworktest;

import com.slg.common.progress.manager.ProgressManager;
import com.slg.entity.mysql.anno.EnableMysql;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
/**
 * 集成测试用 Spring Boot 入口
 * 仅用于 slg-framework-test 模块，扫描各框架包并启用 MySQL（持久化测试时使用）
 *
 * @author framework-test
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.slg.common",
    "com.slg.entity",
    "com.slg.redis",
    "com.slg.frameworktest"
}, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {
        ProgressManager.class,
        FrameworkTestRedisOnlyApplication.class,
        FrameworkTestRedisRouteApplication.class
    }
))
@EntityScan(basePackages = {
    "com.slg.entity.mysql.entity",
    "com.slg.frameworktest.persistence.entity"
})
@EnableMysql
public class FrameworkTestApplication {
}
