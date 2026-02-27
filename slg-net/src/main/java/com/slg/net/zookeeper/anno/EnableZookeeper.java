package com.slg.net.zookeeper.anno;

import com.slg.net.zookeeper.config.ZookeeperConfiguration;
import com.slg.net.zookeeper.config.ZookeeperLifeCycleConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 Zookeeper 配置读取和信息共享支持
 * 通过 @Import 引入 Zookeeper 配置类和生命周期管理
 *
 * <p>使用方式：在启动类上添加 {@code @EnableZookeeper} 注解，
 * 并在模块的 pom.xml 中显式引入 {@code curator-recipes} 依赖
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
        ZookeeperConfiguration.class,
        ZookeeperLifeCycleConfiguration.class
})
public @interface EnableZookeeper {
}
