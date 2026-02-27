package com.slg.net.thrift.codec;

import com.slg.common.log.LoggerUtil;
import com.slg.net.thrift.converter.IThriftConverter;
import com.slg.net.thrift.converter.ThriftConverterRegistry;
import com.slg.net.thrift.converter.ThriftConverterRegistry.ConverterEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.HttpObject;
import org.apache.thrift.TBase;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TIOStreamTransport;

/**
 * Thrift 消息编码器
 * 将服务器内部 POJO 对象转换为 Thrift 二进制发回客户端
 *
 * 消息帧格式（与客户端约定）：
 * +---------------+-------------------+
 * | MsgId (4字节)  | Thrift Body       |
 * +---------------+-------------------+
 * | int32 BE      | TBinary/TCompact  |
 * +---------------+-------------------+
 *
 * @author yangxunan
 * @date 2026/02/26
 */
public class ThriftMessageEncoder extends MessageToByteEncoder<Object> {

    private final ThriftConverterRegistry converterRegistry;
    private final TProtocolFactory protocolFactory;

    public ThriftMessageEncoder(ThriftConverterRegistry converterRegistry, String protocolType) {
        this.converterRegistry = converterRegistry;
        this.protocolFactory = "compact".equalsIgnoreCase(protocolType)
                ? new TCompactProtocol.Factory()
                : new TBinaryProtocol.Factory();
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        if (msg instanceof HttpObject) {
            return false;
        }
        return super.acceptOutboundMessage(msg);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        ConverterEntry<?, ?> entry = converterRegistry.getByPojoType(msg.getClass());
        if (entry == null) {
            LoggerUtil.error("无法将 {} 编码为 Thrift，未找到对应的转换器", msg.getClass().getName());
            return;
        }

        // 1. POJO → Thrift Struct
        TBase<?, ?> thriftStruct = convertToThrift(entry.converter(), msg);
        if (thriftStruct == null) {
            return;
        }

        // 2. 写入消息 ID（4 字节大端）
        out.writeInt(entry.thriftMsgId());

        // 3. 序列化 Thrift 结构体
        try (ByteBufOutputStream outputStream = new ByteBufOutputStream(out)) {
            TIOStreamTransport transport = new TIOStreamTransport(outputStream);
            TProtocol protocol = protocolFactory.getProtocol(transport);
            thriftStruct.write(protocol);
        } catch (Exception e) {
            LoggerUtil.error("Thrift 序列化失败: {}", msg.getClass().getSimpleName(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TBase<?, ?> convertToThrift(IThriftConverter converter, Object pojoMsg) {
        try {
            return (TBase<?, ?>) converter.toThrift(pojoMsg);
        } catch (Exception e) {
            LoggerUtil.error("POJO → Thrift 转换失败: {}", pojoMsg.getClass().getSimpleName(), e);
            return null;
        }
    }
}
