package com.slg.entity.cache.anno;

import java.lang.annotation.*;

/**
 * EntityCache 自动注入注解
 * 标记在 EntityCache 类型的字段上，Spring 会自动注入对应实体类型的缓存实例
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @Service
 * public class UserService {
 *     @EntityCacheInject
 *     private EntityCache<UserEntity> userCache;
 *     
 *     public UserEntity getUser(String id) {
 *         return userCache.findById(id);
 *     }
 * }
 * }
 * </pre>
 * 
 * @author yangxunan
 * @date 2025-12-18
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EntityCacheInject {
}
