package com.slg.net.rpc.route.redis;

import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.codec.MessageWireCodec;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRespone;
import com.slg.net.rpc.facade.RpcRedisFacade;
import com.slg.net.rpc.route.IRpcRouteSupportService;
import com.slg.net.rpc.util.RpcThreadUtil;
import io.lettuce.core.RedisBusyException;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis RPC 路由消费者运行器
 * 负责从本服请求 Stream 和响应 Stream 中消费消息，解码后分发处理
 *
 * <p>两条消费链路（各一条虚拟线程）：
 * <ul>
 *   <li>请求链路：XREADGROUP from rpc:route:{localServerId} → isExpired? skip : RpcRedisFacade.reciveRpcRequest</li>
 *   <li>响应链路：XREADGROUP from rpc:route:resp:{localServerId} → RpcRedisFacade.reciveRpcRespone</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/03/04
 */
public class RpcRedisRouteConsumerRunner implements SmartLifecycle {

    private static final String FIELD_DATA = "data";

    private final RedisTemplate<String, byte[]> routeRedisTemplate;
    private final RpcRedisFacade rpcRedisFacade;
    private final IRpcRouteSupportService rpcRouteSupportService;
    private final RpcRouteRedisProperties properties;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public RpcRedisRouteConsumerRunner(RedisTemplate<String, byte[]> routeRedisTemplate,
                                       RpcRedisFacade rpcRedisFacade,
                                       IRpcRouteSupportService rpcRouteSupportService,
                                       RpcRouteRedisProperties properties) {
        this.routeRedisTemplate = routeRedisTemplate;
        this.rpcRedisFacade = rpcRedisFacade;
        this.rpcRouteSupportService = rpcRouteSupportService;
        this.properties = properties;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        String requestStream = rpcRouteSupportService.getLocalRedisRouteChannel();
        String respStream = rpcRouteSupportService.getLocalRedisRespChannel();
        String group = properties.getConsumerGroup();
        String consumerName = "game-" + rpcRouteSupportService.getLocalServerId();

        ensureGroupExists(requestStream, group);
        ensureGroupExists(respStream, group);

        Thread.ofVirtual().name("rpc-route-req-consumer").start(
                () -> consumeLoop(requestStream, group, consumerName, true));
        Thread.ofVirtual().name("rpc-route-resp-consumer").start(
                () -> consumeLoop(respStream, group, consumerName, false));

        LoggerUtil.info("[RpcRoute] 消费者启动: requestStream={}, respStream={}, group={}, consumer={}",
                requestStream, respStream, group, consumerName);
    }

    @Override
    public void stop() {
        running.set(false);
        LoggerUtil.info("[RpcRoute] 消费者停止信号已发送");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    /**
     * 主消费循环，阻塞读取 Stream 消息并处理
     *
     * @param streamKey Stream key
     * @param group     消费者组
     * @param consumer  消费者名称
     * @param isRequest true=请求链路，false=响应链路
     */
    private void consumeLoop(String streamKey, String group, String consumer, boolean isRequest) {
        @SuppressWarnings("unchecked")
        StreamOperations<String, String, byte[]> streamOps = routeRedisTemplate.opsForStream();

        long blockMs = properties.getBlockMillis();
        StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(properties.getBatchSize())
                .block(Duration.ofMillis(blockMs));

        while (running.get()) {
            try {
                List<MapRecord<String, String, byte[]>> records = streamOps.read(
                        Consumer.from(group, consumer),
                        readOptions,
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                );

                if (records == null || records.isEmpty()) {
                    continue;
                }

                List<RecordId> processedIds = new ArrayList<>(records.size());
                for (MapRecord<String, String, byte[]> record : records) {
                    processRecord(record, isRequest);
                    processedIds.add(record.getId());
                }
                pipelinedAckAndDelete(streamKey, group, processedIds);

            } catch (Exception e) {
                if (running.get()) {
                    LoggerUtil.error("[RpcRoute] 消费循环异常: stream={}, isRequest={}",
                            streamKey, isRequest, e);
                    sleepQuietly(1000);
                }
            }
        }
        LoggerUtil.info("[RpcRoute] 消费循环已退出: stream={}", streamKey);
    }

    /**
     * 处理单条 Stream 记录：解码消息，按链路类型分发
     * ACK/DELETE 由外层批量执行，此处仅负责解码与业务分发
     */
    private void processRecord(MapRecord<String, String, byte[]> record, boolean isRequest) {
        RecordId recordId = record.getId();
        try {
            byte[] data = record.getValue().get(FIELD_DATA);
            if (data == null || data.length == 0) {
                LoggerUtil.warn("[RpcRoute] Stream 记录无效，跳过: recordId={}", recordId);
                return;
            }

            Object message = MessageWireCodec.decode(data);

            if (isRequest) {
                IM_RpcRequest request = (IM_RpcRequest) message;
                if (request.isExpired()) {
                    LoggerUtil.warn("[RpcRoute] 跳过过期请求: method={}, 剩余: {}ms",
                            request.getMethodMarker(), request.getRemainingTimeMillis());
                    return;
                }
                RpcThreadUtil.dispatch(request, () -> rpcRedisFacade.reciveRpcRequest(request));
            } else {
                Executor.RpcResponse.execute(() ->
                        rpcRedisFacade.reciveRpcRespone((IM_RpcRespone) message));
            }

        } catch (Exception e) {
            LoggerUtil.error("[RpcRoute] 处理 Stream 记录异常，跳过: recordId={}", recordId, e);
        }
    }

    /**
     * 确保消费者组存在（Stream 不存在时通过 MKSTREAM 自动创建）
     */
    private void ensureGroupExists(String streamKey, String group) {
        try {
            byte[] rawKey = streamKey.getBytes(StandardCharsets.UTF_8);
            routeRedisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.streamCommands().xGroupCreate(rawKey, group, ReadOffset.from("0"), true);
                return null;
            });
            LoggerUtil.info("[RpcRoute] 消费者组已创建: stream={}, group={}", streamKey, group);
        } catch (Exception e) {
            if (e.getCause() instanceof RedisBusyException) {
                LoggerUtil.debug("[RpcRoute] 消费者组已存在: stream={}, group={}", streamKey, group);
            } else {
                LoggerUtil.warn("[RpcRoute] 消费者组创建异常: stream={}, group={}", streamKey, group, e);
            }
        }
    }

    /**
     * 通过 Pipeline 将 XACK 和 XDEL 合并到同一次网络往返，比两次独立调用减少一次 RTT
     */
    private void pipelinedAckAndDelete(String streamKey, String group, List<RecordId> ids) {
        if (ids.isEmpty()) {
            return;
        }
        try {
            byte[] rawKey = streamKey.getBytes(StandardCharsets.UTF_8);
            RecordId[] idArray = ids.toArray(new RecordId[0]);
            routeRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                connection.streamCommands().xAck(rawKey, group, idArray);
                connection.streamCommands().xDel(rawKey, idArray);
                return null;
            });
        } catch (Exception e) {
            LoggerUtil.error("[RpcRoute] Pipeline XACK/XDEL 异常: stream={}, count={}", streamKey, ids.size(), e);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
