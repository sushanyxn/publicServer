package com.slg.net.syncbus.anno;

import com.slg.net.syncbus.codec.ISyncFieldDecoder;
import com.slg.net.syncbus.codec.ISyncFieldEncoder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级别注解，标记需要同步的字段
 * 两端字段名需一一对应（由开发者保证）
 *
 * @author yangxunan
 * @date 2026/02/12
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SyncField {

    /**
     * 自定义编码器类（仅在 Holder 端生效）
     * 未指定时使用默认 JsonUtil.toJson() 序列化
     *
     * @return 编码器类，默认为 ISyncFieldEncoder.class 表示不使用自定义编码器
     */
    @SuppressWarnings("rawtypes")
    Class<? extends ISyncFieldEncoder> encoder() default ISyncFieldEncoder.class;

    /**
     * 自定义解码器类（仅在 Cache 端生效）
     * 未指定时使用默认 JsonUtil.fromJson() 反序列化
     *
     * @return 解码器类，默认为 ISyncFieldDecoder.class 表示不使用自定义解码器
     */
    @SuppressWarnings("rawtypes")
    Class<? extends ISyncFieldDecoder> decoder() default ISyncFieldDecoder.class;

    /**
     * 同步限流间隔（秒），仅在 Holder 端生效
     * <ul>
     *   <li>0：不限流，每次 sync() 立即发送</li>
     *   <li>N（N > 0）：N 秒内该字段最多实际发送一次</li>
     * </ul>
     * 默认 1 秒
     *
     * @return 限流间隔秒数
     */
    int syncInterval() default 1;
}
