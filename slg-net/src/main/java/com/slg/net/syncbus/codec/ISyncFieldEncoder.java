package com.slg.net.syncbus.codec;

/**
 * 自定义字段编码器接口（Holder 端使用）
 * 将 Holder 端的字段值编码为字符串，用于跨进程传输
 * <p>
 * 实现类定义在 Holder 所在模块（如 slg-game），面向 Holder 端的 model 工作
 * 必须有无参构造器，框架启动时自动实例化并缓存
 *
 * @param <T> 字段值类型
 * @author yangxunan
 * @date 2026/02/12
 */
public interface ISyncFieldEncoder<T> {

    /**
     * 将字段值编码为字符串
     *
     * @param value 字段值
     * @return 编码后的字符串
     */
    String encode(T value);
}
