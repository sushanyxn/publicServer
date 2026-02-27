package com.slg.net.thrift.codec;

import com.slg.common.log.LoggerUtil;
import com.slg.net.thrift.converter.IThriftConverter;
import com.slg.net.thrift.converter.ThriftConverterRegistry;
import com.slg.net.thrift.converter.ThriftConverterRegistry.ConverterEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TIOStreamTransport;

import java.util.List;

/**
 * Thrift 消息解码器
 * 将 Thrift 二进制数据转换为服务器内部 POJO 对象
 *
 * 消息帧格式（与客户端约定）：
 * +---------------+-------------------+
 * | MsgId (4字节)  | Thrift Body       |
 * +---------------+-------------------+
 * | int32 BE      | TBinary/TCompact  |
 * +---------------+-------------------+
 *
 * 每个 WebSocket 帧恰好包含一条完整消息
 *
 * @author yangxunan
 * @date 2026/02/26
 */
public class ThriftMessageDecoder extends ByteToMessageDecoder {

    private final ThriftConverterRegistry converterRegistry;
    private final TProtocolFactory protocolFactory;

    public ThriftMessageDecoder(ThriftConverterRegistry converterRegistry, String protocolType) {
        this.converterRegistry = converterRegistry;
        this.protocolFactory = "compact".equalsIgnoreCase(protocolType)
                ? new TCompactProtocol.Factory()
                : new TBinaryProtocol.Factory();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }

        in.markReaderIndex();

        // 1. 读取消息 ID（4 字节大端）
        int thriftMsgId = in.readInt();

        // 2. 查找转换器
        ConverterEntry<?, ?> entry = converterRegistry.getByThriftId(thriftMsgId);
        if (entry == null) {
            LoggerUtil.error("未知的 Thrift 消息 ID: {}", thriftMsgId);
            in.resetReaderIndex();
            in.skipBytes(in.readableBytes());
            return;
        }

        // 3. 反序列化 Thrift 结构体
        TBase<?, ?> thriftStruct = createThriftInstance(entry.converter().getThriftType());
        if (thriftStruct == null) {
            in.skipBytes(in.readableBytes());
            return;
        }

        try (ByteBufInputStream inputStream = new ByteBufInputStream(in)) {
            TIOStreamTransport transport = new TIOStreamTransport(inputStream);
            TProtocol protocol = protocolFactory.getProtocol(transport);
            thriftStruct.read(protocol);
        } catch (Exception e) {
            LoggerUtil.error("Thrift 反序列化失败，msgId={}, type={}",
                    thriftMsgId, entry.converter().getThriftType().getSimpleName(), e);
            return;
        }

        // 4. 转换为 POJO
        Object pojo = convertToPojo(entry.converter(), thriftStruct);
        if (pojo != null) {
            out.add(pojo);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertToPojo(IThriftConverter converter, TBase thriftStruct) {
        try {
            return converter.fromThrift(thriftStruct);
        } catch (Exception e) {
            LoggerUtil.error("Thrift → POJO 转换失败: {}", thriftStruct.getClass().getSimpleName(), e);
            return null;
        }
    }

    private TBase<?, ?> createThriftInstance(Class<? extends TBase> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LoggerUtil.error("创建 Thrift 实例失败: {}", clazz.getName(), e);
            return null;
        }
    }
}
