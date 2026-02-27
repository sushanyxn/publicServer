package com.slg.net.thrift.converter;

import org.apache.thrift.TBase;

/**
 * Thrift 协议转换器接口
 * 定义 Thrift 生成类与服务器内部 POJO 之间的双向转换
 *
 * @param <T> Thrift 生成的消息类型
 * @param <R> 服务器内部 POJO 类型
 * @author yangxunan
 * @date 2026/02/26
 */
public interface IThriftConverter<T extends TBase<?, ?>, R> {

    /**
     * 将 Thrift 消息转换为内部 POJO（入站：客户端 → 服务器）
     *
     * @param thriftMsg Thrift 消息对象
     * @return 转换后的 POJO 对象
     */
    R fromThrift(T thriftMsg);

    /**
     * 将内部 POJO 转换为 Thrift 消息（出站：服务器 → 客户端）
     *
     * @param pojoMsg 内部 POJO 对象
     * @return 转换后的 Thrift 消息对象
     */
    T toThrift(R pojoMsg);

    /**
     * 获取 Thrift 消息类型
     */
    Class<T> getThriftType();

    /**
     * 获取内部 POJO 消息类型
     */
    Class<R> getPojoType();
}
