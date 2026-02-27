package com.slg.redis.cache.anno;

import com.slg.redis.cache.codec.ICacheFieldCodec;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级别注解，标记参与 Redis Hash 缓存的字段
 * 只有标注了此注解的字段才会被缓存系统读写
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheField {

    /**
     * 自定义编解码器类
     * 未指定时使用默认的 JSON 编解码（{@link com.slg.redis.cache.codec.JsonCacheFieldCodec}）
     *
     * @return 编解码器类，默认为 ICacheFieldCodec.class 表示使用默认编解码
     */
    @SuppressWarnings("rawtypes")
    Class<? extends ICacheFieldCodec> codec() default ICacheFieldCodec.class;
}
