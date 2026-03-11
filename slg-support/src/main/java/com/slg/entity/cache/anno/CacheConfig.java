package com.slg.entity.cache.anno;

import java.lang.annotation.*;

/**
 * 实体缓存配置注解
 * 标记在实体类上，用于配置该实体的缓存参数
 * 
 * 缓存策略：
 * - 当缓存数量超过最大值时，按 LRU（最久未使用）策略淘汰
 * - 过期时间从最后一次访问开始计算
 * - 支持 -1 表示不限制大小或不过期
 * 
 *
 * @author yangxunan
 * @date 2025-12-18
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {
    
    /**
     * 最大缓存数量
     * 超过此数量时，按 LRU 策略淘汰最久未使用的实体
     * 
     * @return 最大缓存数量，默认 5000
     *         -1 表示不限制大小
     */
    long maxSize() default 5000;
    
    /**
     * 过期时间（分钟）
     * 实体在最后一次访问后，超过此时间将过期
     * 
     * @return 过期时间（分钟），默认 30 分钟
     *         -1 表示永不过期
     */
    long expireMinutes() default 30;
    
    /**
     * 是否在缓存过期时自动保存到数据库
     * 启用此选项后，当实体因过期被移除时，会自动调用 save() 保存到数据库
     *
     * @return true 表示自动保存，默认 true
     */
    boolean autoSaveOnExpire() default true;
    
    /**
     * 是否启用 Write-Behind 模式
     * 启用后，所有写入操作会先更新缓存，然后异步批量写入数据库
     *
     * @return true 表示启用 Write-Behind，默认 true
     */
    boolean writeDelay() default true;
    
    /**
     * Write-Behind 批量写入间隔（秒）
     * 只在 writeBehind = true 时生效
     *
     * @return 批量写入间隔（秒），默认 60 秒
     */
    long writeDelaySec() default 60;

    /**
     * 开启批量入库
     * 只有开启了Write-Behind 批量写入, 才会生效
     * @return
     */
    int batchSaveSize() default 50;

    /**
     * 缓存未命中时是否跳过数据库查询
     * 为 true 时，findById 在缓存中查不到则直接返回 null，不再访问数据库。
     * 适用于「永不过期、全量预加载」的实体（如 AccountEntity），未命中即表示不存在。
     *
     * @return true 表示不查库，默认 false（未命中时仍从数据库加载）
     */
    boolean skipDbOnMiss() default false;
}
