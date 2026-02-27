package com.slg.redis.cache.codec;

/**
 * 缓存字段编解码接口
 * 负责将字段值与 Redis Hash 中的字符串进行互相转换
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public interface ICacheFieldCodec<T> {

    /**
     * 将字段值编码为字符串，用于写入 Redis Hash
     *
     * @param value 字段值
     * @return 编码后的字符串
     */
    String encode(T value);

    /**
     * 将 Redis Hash 中的字符串解码为字段值
     *
     * @param raw  Redis 中存储的原始字符串
     * @param type 目标类型
     * @return 解码后的字段值
     */
    T decode(String raw, Class<T> type);
}
