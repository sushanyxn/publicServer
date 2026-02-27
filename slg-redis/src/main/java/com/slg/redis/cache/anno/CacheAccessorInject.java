package com.slg.redis.cache.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CacheAccessor 自动注入注解
 * 标记在 {@link com.slg.redis.cache.accessor.CacheAccessor} 类型的字段上，
 * 框架根据泛型参数自动注入对应实体类型的缓存访问器
 *
 * <p>使用示例：
 * <pre>{@code
 * @Component
 * public class SomeService {
 *     @CacheAccessorInject
 *     private CacheAccessor<PlayerCacheObj> playerCache;
 * }
 * }</pre>
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheAccessorInject {
}
