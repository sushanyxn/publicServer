package com.slg.net.thrift.converter;

import com.slg.common.log.LoggerUtil;
import org.apache.thrift.TBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Thrift 转换器注册中心
 * 维护 Thrift 消息 ID / 类型 与 IThriftConverter 的映射关系
 *
 * @author yangxunan
 * @date 2026/02/26
 */
public class ThriftConverterRegistry {

    /**
     * Thrift 消息 ID → 转换器（入站解码使用）
     */
    private final Map<Integer, ConverterEntry<?, ?>> thriftIdToEntry = new HashMap<>();

    /**
     * POJO 类型 → 转换器条目（出站编码使用）
     */
    private final Map<Class<?>, ConverterEntry<?, ?>> pojoTypeToEntry = new HashMap<>();

    /**
     * 注册一组映射：thriftMsgId ↔ thriftClass ↔ pojoClass，使用自动反射转换器
     *
     * @param thriftMsgId Thrift 协议使用的消息 ID
     * @param thriftClass Thrift 生成的 Java 类
     * @param pojoClass   服务器内部 POJO 类
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerAutoMapping(int thriftMsgId, Class<? extends TBase> thriftClass, Class<?> pojoClass) {
        ReflectiveThriftConverter converter = new ReflectiveThriftConverter(thriftClass, pojoClass);
        register(thriftMsgId, converter);
    }

    /**
     * 注册手动编写的转换器
     *
     * @param thriftMsgId Thrift 协议使用的消息 ID
     * @param converter   转换器实例
     */
    public void register(int thriftMsgId, IThriftConverter<?, ?> converter) {
        ConverterEntry<?, ?> entry = new ConverterEntry<>(thriftMsgId, converter);
        thriftIdToEntry.put(thriftMsgId, entry);
        pojoTypeToEntry.put(converter.getPojoType(), entry);

        LoggerUtil.debug("注册 Thrift 转换器: thriftMsgId={}, thrift={}, pojo={}",
                thriftMsgId, converter.getThriftType().getSimpleName(), converter.getPojoType().getSimpleName());
    }

    /**
     * 根据 Thrift 消息 ID 获取转换器条目（入站）
     */
    public ConverterEntry<?, ?> getByThriftId(int thriftMsgId) {
        return thriftIdToEntry.get(thriftMsgId);
    }

    /**
     * 根据 POJO 类型获取转换器条目（出站）
     */
    public ConverterEntry<?, ?> getByPojoType(Class<?> pojoType) {
        return pojoTypeToEntry.get(pojoType);
    }

    /**
     * 获取已注册的转换器数量
     */
    public int size() {
        return thriftIdToEntry.size();
    }

    /**
     * 转换器条目，包含消息 ID 和转换器实例
     */
    public record ConverterEntry<T extends TBase<?, ?>, R>(
            int thriftMsgId,
            IThriftConverter<T, R> converter
    ) {
    }
}
