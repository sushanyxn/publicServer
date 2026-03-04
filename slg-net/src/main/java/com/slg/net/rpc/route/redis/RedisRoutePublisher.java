package com.slg.net.rpc.route.redis;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.codec.MessageWireCodec;
import com.slg.net.rpc.route.IRpcRouteSupportService;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Redis 路由消息发布器
 * 负责将编码后的 RPC 消息通过 XADD 写入目标服务器的 Redis Stream
 *
 * @author yangxunan
 * @date 2026/03/04
 */
public class RedisRoutePublisher {

    private static final String FIELD_DATA = "data";

    private final RedisTemplate<String, byte[]> routeRedisTemplate;
    private final IRpcRouteSupportService rpcRouteSupportService;
    private final long streamMaxLen;

    public RedisRoutePublisher(RedisTemplate<String, byte[]> routeRedisTemplate,
                               IRpcRouteSupportService rpcRouteSupportService,
                               RpcRouteRedisProperties properties) {
        this.routeRedisTemplate = routeRedisTemplate;
        this.rpcRouteSupportService = rpcRouteSupportService;
        this.streamMaxLen = properties.getStreamMaxLen();
    }

    /**
     * 将消息对象编码并发布到目标服务器的请求 Stream
     *
     * @param targetServerId 目标服务器ID
     * @param message        要发送的消息对象（需已注册协议号）
     */
    public void publish(int targetServerId, Object message) {
        String channel = rpcRouteSupportService.getRedisRouteChannel(targetServerId);
        byte[] encoded = MessageWireCodec.encode(message);
        xadd(channel, encoded);
    }

    /**
     * 将消息对象编码并发布到调用方服务器的响应 Stream
     *
     * @param callerServerId 发起调用的服务器ID（即响应的目标）
     * @param message        要发送的消息对象
     */
    public void publishResp(int callerServerId, Object message) {
        String channel = "rpc:route:resp:" + callerServerId;
        byte[] encoded = MessageWireCodec.encode(message);
        xadd(channel, encoded);
    }

    /**
     * 直接发布原始字节数据到目标服务器的请求 Stream（供内部使用）
     *
     * @param targetServerId 目标服务器ID
     * @param encodedBytes   已编码的字节数组
     */
    public void publishRaw(int targetServerId, byte[] encodedBytes) {
        String channel = rpcRouteSupportService.getRedisRouteChannel(targetServerId);
        xadd(channel, encodedBytes);
    }

    private void xadd(String streamKey, byte[] data) {
        byte[] rawKey = streamKey.getBytes(StandardCharsets.UTF_8);
        byte[] rawField = FIELD_DATA.getBytes(StandardCharsets.UTF_8);
        MapRecord<byte[], byte[], byte[]> rawRecord = StreamRecords.rawBytes(Map.of(rawField, data))
                .withStreamKey(rawKey);
        RedisStreamCommands.XAddOptions options = RedisStreamCommands.XAddOptions
                .maxlen(streamMaxLen).approximateTrimming(true);
        routeRedisTemplate.execute((RedisCallback<RecordId>) connection ->
                connection.streamCommands().xAdd(rawRecord, options));
    }
}
