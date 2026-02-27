package com.slg.net.syncbus.codec;

/**
 * 自定义字段解码器接口（Cache 端使用）
 * 将字符串解码为 Cache 端的字段值
 * <p>
 * 实现类定义在 Cache 所在模块（如 slg-scene），面向 Cache 端的 model 工作
 * 必须有无参构造器，框架启动时自动实例化并缓存
 *
 * @param <T> 字段值类型
 * @author yangxunan
 * @date 2026/02/12
 */
public interface ISyncFieldDecoder<T> {

    /**
     * 将字符串解码为字段值
     *
     * @param data 编码后的字符串数据
     * @return 解码后的字段值
     */
    T decode(String data);
}
