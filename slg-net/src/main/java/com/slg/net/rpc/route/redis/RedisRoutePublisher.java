package com.slg.net.rpc.route.redis;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.codec.MessageWireCodec;
import com.slg.net.rpc.route.IRpcRouteSupportService;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Redis 路由消息发布器（Pipeline 模式）
 * 将 RPC 消息通过 Lettuce 异步 Pipeline 批量写入目标服务器的 Redis Stream
 *
 * <p><b>传输可靠性策略</b>：
 * <ul>
 *   <li><b>可靠模式</b>（{@link #publish}, {@link #publishResp}）：Pipeline 批量 XADD 后等待
 *       Redis 返回所有 RecordId，确保消息写入 Stream。用于有返回值的 RPC 方法和所有响应消息。</li>
 *   <li><b>Fire-and-Forget 模式</b>（{@link #publishFireAndForget}）：Pipeline 批量 XADD 后
 *       不等待确认，与可靠消息共享同一次 flushCommands() 网络往返。用于 void RPC 方法——
 *       因为 void 方法的 callBackId = 0，发送方不注册回调也不持有 Future，
 *       即使消息丢失也不会产生悬挂状态。</li>
 * </ul>
 *
 * <p><b>刷新触发条件</b>（满足任一即触发）：
 * <ol>
 *   <li>缓冲区消息数 ≥ pipelineBatchSize</li>
 *   <li>缓冲区累计字节数 ≥ pipelineBatchMaxBytes</li>
 *   <li>距上次刷新超过 pipelineFlushIntervalMs（定时兜底）</li>
 * </ol>
 *
 * @author yangxunan
 * @date 2026/03/04
 */
public class RedisRoutePublisher implements SmartLifecycle {

    private static final byte[] FIELD_DATA_BYTES = "data".getBytes(StandardCharsets.UTF_8);
    private static final int MAX_RETRY = 3;

    private final IRpcRouteSupportService rpcRouteSupportService;

    private final RedisClient redisClient;
    private final StatefulRedisConnection<byte[], byte[]> connection;
    private final RedisAsyncCommands<byte[], byte[]> asyncCommands;
    private final XAddArgs xAddArgs;

    private final ConcurrentLinkedQueue<PendingMessage> reliableBuffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PendingMessage> fireAndForgetBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingBytes = new AtomicInteger(0);
    private final AtomicInteger pendingCount = new AtomicInteger(0);

    private final int pipelineBatchSize;
    private final int pipelineBatchMaxBytes;
    private final long pipelineFlushIntervalMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ReentrantLock flushLock = new ReentrantLock();

    public RedisRoutePublisher(IRpcRouteSupportService rpcRouteSupportService,
                               RpcRouteRedisProperties properties) {
        this.rpcRouteSupportService = rpcRouteSupportService;
        this.pipelineBatchSize = properties.getPipelineBatchSize();
        this.pipelineBatchMaxBytes = properties.getPipelineBatchMaxBytes();
        this.pipelineFlushIntervalMs = properties.getPipelineFlushIntervalMs();
        this.xAddArgs = new XAddArgs().maxlen(properties.getStreamMaxLen()).approximateTrimming();

        this.redisClient = createRedisClient(properties);
        this.connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        this.asyncCommands = connection.async();
        this.connection.setAutoFlushCommands(false);
    }

    /**
     * 将消息对象编码并发布到目标服务器的请求 Stream（可靠模式）
     * 用于有返回值的 RPC 方法，flush 时等待 Redis 写入确认
     *
     * @param targetServerId 目标服务器ID
     * @param message        要发送的消息对象（需已注册协议号）
     */
    public void publish(int targetServerId, Object message) {
        byte[] streamKey = rpcRouteSupportService.getRedisRouteChannel(targetServerId)
                .getBytes(StandardCharsets.UTF_8);
        byte[] encoded = MessageWireCodec.encode(message);
        enqueue(reliableBuffer, streamKey, encoded);
    }

    /**
     * 将消息对象编码并发布到目标服务器的请求 Stream（fire-and-forget 模式）
     * 用于 void RPC 方法，flush 时不等待写入确认，与可靠消息共享同一次网络往返
     *
     * @param targetServerId 目标服务器ID
     * @param message        要发送的消息对象（需已注册协议号）
     */
    public void publishFireAndForget(int targetServerId, Object message) {
        byte[] streamKey = rpcRouteSupportService.getRedisRouteChannel(targetServerId)
                .getBytes(StandardCharsets.UTF_8);
        byte[] encoded = MessageWireCodec.encode(message);
        enqueue(fireAndForgetBuffer, streamKey, encoded);
    }

    /**
     * 将消息对象编码并发布到调用方服务器的响应 Stream（始终可靠模式）
     * 响应消息关乎调用方回调是否超时，必须确认写入
     *
     * @param callerServerId 发起调用的服务器ID（即响应的目标）
     * @param message        要发送的消息对象
     */
    public void publishResp(int callerServerId, Object message) {
        byte[] streamKey = ("rpc:route:resp:" + callerServerId).getBytes(StandardCharsets.UTF_8);
        byte[] encoded = MessageWireCodec.encode(message);
        enqueue(reliableBuffer, streamKey, encoded);
    }

    /**
     * 直接发布原始字节数据到目标服务器的请求 Stream（可靠模式，供测试使用）
     *
     * @param targetServerId 目标服务器ID
     * @param encodedBytes   已编码的字节数组
     */
    public void publishRaw(int targetServerId, byte[] encodedBytes) {
        byte[] streamKey = rpcRouteSupportService.getRedisRouteChannel(targetServerId)
                .getBytes(StandardCharsets.UTF_8);
        enqueue(reliableBuffer, streamKey, encodedBytes);
    }

    /**
     * 强制刷新缓冲区，阻塞直到所有缓冲消息发送完毕
     * 可供外部调用以确保缓冲消息全部写入 Redis（如测试场景或停机前检查点）
     */
    public void flush() {
        flushLock.lock();
        try {
            doFlush();
        } finally {
            flushLock.unlock();
        }
    }

    // ======================== SmartLifecycle ========================

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual().name("rpc-route-flusher").start(this::flushLoop);
        LoggerUtil.info("[RpcRoute] Publisher 已启动 (Pipeline 模式, batchSize={}, batchMaxBytes={}, flushInterval={}ms)",
                pipelineBatchSize, pipelineBatchMaxBytes, pipelineFlushIntervalMs);
    }

    @Override
    public void stop() {
        running.set(false);
        flush();
        try {
            connection.close();
            redisClient.shutdown(Duration.ZERO, Duration.ofSeconds(2));
        } catch (Exception e) {
            LoggerUtil.warn("[RpcRoute] Publisher 关闭连接异常", e);
        }
        LoggerUtil.info("[RpcRoute] Publisher 已停止");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 200;
    }

    // ======================== 内部实现 ========================

    private void enqueue(ConcurrentLinkedQueue<PendingMessage> buffer, byte[] streamKey, byte[] data) {
        buffer.offer(new PendingMessage(streamKey, data));
        int count = pendingCount.incrementAndGet();
        int bytes = pendingBytes.addAndGet(data.length);
        if (count >= pipelineBatchSize || bytes >= pipelineBatchMaxBytes) {
            tryFlush();
        }
    }

    private void tryFlush() {
        if (flushLock.tryLock()) {
            try {
                doFlush();
            } finally {
                flushLock.unlock();
            }
        }
    }

    private void doFlush() {
        if (reliableBuffer.isEmpty() && fireAndForgetBuffer.isEmpty()) {
            return;
        }

        List<PendingMessage> reliableMessages = new ArrayList<>();
        List<RedisFuture<String>> reliableFutures = new ArrayList<>();
        PendingMessage msg;

        while ((msg = reliableBuffer.poll()) != null) {
            reliableMessages.add(msg);
            reliableFutures.add(asyncCommands.xadd(msg.streamKey, xAddArgs, FIELD_DATA_BYTES, msg.data));
        }
        while ((msg = fireAndForgetBuffer.poll()) != null) {
            asyncCommands.xadd(msg.streamKey, xAddArgs, FIELD_DATA_BYTES, msg.data);
        }

        pendingCount.set(0);
        pendingBytes.set(0);

        try {
            connection.flushCommands();
        } catch (Exception e) {
            LoggerUtil.error("[RpcRoute] flushCommands 异常，重投可靠消息, count={}", reliableMessages.size(), e);
            requeueFailed(reliableMessages, null);
            return;
        }

        if (!reliableFutures.isEmpty()) {
            try {
                boolean allDone = LettuceFutures.awaitAll(5, TimeUnit.SECONDS,
                        reliableFutures.toArray(new RedisFuture[0]));
                if (!allDone) {
                    int requeued = requeueFailed(reliableMessages, reliableFutures);
                    LoggerUtil.error("[RpcRoute] Pipeline flush 超时，重投 {} 条可靠消息", requeued);
                }
            } catch (Exception e) {
                int requeued = requeueFailed(reliableMessages, reliableFutures);
                LoggerUtil.error("[RpcRoute] Pipeline flush 异常，重投 {} 条可靠消息", requeued, e);
            }
        }
    }

    /**
     * 将失败的可靠消息重新入队
     * futures 为 null 时视为全部失败（如 flushCommands 异常）；否则逐个检查 Future 状态
     *
     * @return 实际重投条数
     */
    private int requeueFailed(List<PendingMessage> messages, List<RedisFuture<String>> futures) {
        int requeued = 0;
        for (int i = 0; i < messages.size(); i++) {
            boolean success = false;
            if (futures != null) {
                RedisFuture<String> future = futures.get(i);
                if (future.isDone()) {
                    try {
                        future.get();
                        success = true;
                    } catch (Exception ignored) {
                    }
                }
            }
            if (!success) {
                PendingMessage msg = messages.get(i);
                if (msg.retryCount >= MAX_RETRY) {
                    LoggerUtil.error("[RpcRoute] 可靠消息重试 {} 次仍失败，丢弃: streamKey={}",
                            MAX_RETRY, new String(msg.streamKey, java.nio.charset.StandardCharsets.UTF_8));
                    continue;
                }
                reliableBuffer.offer(msg.retry());
                pendingCount.incrementAndGet();
                pendingBytes.addAndGet(msg.data.length);
                requeued++;
            }
        }
        return requeued;
    }

    private void flushLoop() {
        while (running.get()) {
            try {
                Thread.sleep(pipelineFlushIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            tryFlush();
        }
    }

    private static RedisClient createRedisClient(RpcRouteRedisProperties properties) {
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(properties.getHost())
                .withPort(properties.getPort())
                .withDatabase(properties.getDatabase())
                .withTimeout(Duration.ofMillis(properties.getTimeout()));
        if (StringUtils.hasText(properties.getPassword())) {
            uriBuilder.withPassword(properties.getPassword().toCharArray());
        }
        return RedisClient.create(uriBuilder.build());
    }

    private record PendingMessage(byte[] streamKey, byte[] data, int retryCount) {
        PendingMessage(byte[] streamKey, byte[] data) {
            this(streamKey, data, 0);
        }

        PendingMessage retry() {
            return new PendingMessage(streamKey, data, retryCount + 1);
        }
    }
}
