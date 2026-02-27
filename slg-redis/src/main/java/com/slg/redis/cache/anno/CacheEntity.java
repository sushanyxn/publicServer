package com.slg.redis.cache.anno;

import com.slg.redis.cache.CacheModule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类级别注解，声明该类为 Redis Hash 缓存实体
 * 标注在缓存对象类上，指定所属的 {@link CacheModule}
 * <p>每个 CacheModule 只能对应一个 @CacheEntity 类
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEntity {

    /**
     * 所属的缓存模块
     *
     * @return CacheModule 枚举值
     */
    CacheModule module();
}
