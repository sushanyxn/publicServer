package com.slg.frameworktest.performance;

import com.slg.net.rpc.route.redis.RedisRoutePublisher;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Route 性能测试
 * <p>
 * 测试场景：
 * <ol>
 *   <li>单点大量写入 — 不同消息体积下的 XADD 吞吐与延迟</li>
 *   <li>多节点转发 — 目标节点数量增加时的写入性能变化</li>
 *   <li>双向互写 — 两侧同时 publish + consume 的吞吐表现</li>
 * </ol>
 *
 * @author framework-test
 * @date 2026/03/05
 */
@SpringBootTest(classes = com.slg.frameworktest.FrameworkTestRedisRouteApplication.class)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisRoutePerformanceTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        String host = redis.getHost();
        String port = redis.getMappedPort(6379).toString();
        registry.add("spring.data.redis.host", () -> host);
        registry.add("spring.data.redis.port", () -> port);
        registry.add("rpc.route.redis.host", () -> host);
        registry.add("rpc.route.redis.port", () -> port);
    }

    @Autowired
    private RedisRoutePublisher redisRoutePublisher;

    @Autowired
    private RedisTemplate<String, byte[]> routeRedisTemplate;

    private static final int WARM_UP_COUNT = 50;

    private static byte[] buildPayload(int sizeBytes) {
        byte[] payload = new byte[sizeBytes];
        Arrays.fill(payload, (byte) 'A');
        return payload;
    }

    private void warmUp(int targetServerId) {
        byte[] small = buildPayload(64);
        for (int i = 0; i < WARM_UP_COUNT; i++) {
            redisRoutePublisher.publishRaw(targetServerId, small);
        }
    }

    private void cleanStream(String streamKey) {
        try {
            routeRedisTemplate.delete(streamKey);
        } catch (Exception ignored) {
        }
    }

    // ======================== 场景一：单点大量写入，消息体积递增 ========================

    @Test
    @Order(1)
    @DisplayName("场景一：单点写入吞吐 — 消息体积 64B / 256B / 1KB / 4KB / 16KB")
    void singlePointThroughputByPayloadSize() {
        int targetServerId = 100;
        int messagesPerRound = 2000;
        int[] payloadSizes = {64, 256, 1024, 4096, 16384};

        warmUp(targetServerId);

        System.out.println();
        System.out.println("=== 场景一：单点写入 — 消息体积 vs 吞吐 ===");
        System.out.printf("%-12s %-12s %-14s %-14s %-12s%n",
                "Payload", "Count", "Elapsed(ms)", "Ops/sec", "Avg(μs)");
        System.out.println("-".repeat(66));

        for (int size : payloadSizes) {
            String streamKey = "rpc:route:" + targetServerId;
            cleanStream(streamKey);

            byte[] payload = buildPayload(size);
            long start = System.nanoTime();
            for (int i = 0; i < messagesPerRound; i++) {
                redisRoutePublisher.publishRaw(targetServerId, payload);
            }
            long elapsedNs = System.nanoTime() - start;
            long elapsedMs = elapsedNs / 1_000_000;
            double opsPerSec = messagesPerRound * 1_000_000_000.0 / elapsedNs;
            long avgUs = elapsedNs / messagesPerRound / 1_000;

            System.out.printf("%-12s %-12d %-14d %-14.0f %-12d%n",
                    formatSize(size), messagesPerRound, elapsedMs, opsPerSec, avgUs);

            Long len = routeRedisTemplate.opsForStream().size(streamKey);
            assertThat(len).isGreaterThanOrEqualTo(1L);
        }
        System.out.println();
    }

    @Test
    @Order(2)
    @DisplayName("场景一补充：单点写入延迟分布（P50/P90/P99）— 1KB 消息")
    void singlePointLatencyDistribution() {
        int targetServerId = 101;
        int count = 1000;
        byte[] payload = buildPayload(1024);

        warmUp(targetServerId);
        cleanStream("rpc:route:" + targetServerId);

        List<Long> latenciesUs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long t0 = System.nanoTime();
            redisRoutePublisher.publishRaw(targetServerId, payload);
            latenciesUs.add((System.nanoTime() - t0) / 1_000);
        }
        latenciesUs.sort(Long::compareTo);

        long p50 = latenciesUs.get(count / 2);
        long p90 = latenciesUs.get((int) (count * 0.90));
        long p99 = latenciesUs.get((int) (count * 0.99));
        long max = latenciesUs.get(count - 1);

        System.out.println();
        System.out.println("=== 场景一补充：单点写入延迟分布 (1KB payload, " + count + " msgs) ===");
        System.out.printf("  P50: %d μs | P90: %d μs | P99: %d μs | Max: %d μs%n", p50, p90, p99, max);
        System.out.println();

        assertThat(p99).as("P99 延迟应小于 20ms").isLessThan(20_000);
    }

    // ======================== 场景二：多节点转发 ========================

    @Test
    @Order(3)
    @DisplayName("场景二：多节点转发 — 目标节点 1/2/4/8/16/50/100/200/500 的写入吞吐对比")
    void multiNodeForwardingThroughput() {
        int totalMessages = 5000;
        int[] nodeCounts = {1, 2, 4, 8, 16, 50, 100, 200, 500};
        int baseServerId = 200;
        byte[] payload = buildPayload(1024);

        System.out.println();
        System.out.println("=== 场景二：多节点转发 — 节点数 vs 吞吐 ===");
        System.out.printf("%-10s %-14s %-14s %-14s %-12s%n",
                "Nodes", "MsgsPerNode", "Elapsed(ms)", "Total Ops/s", "Avg(μs)");
        System.out.println("-".repeat(66));

        for (int nodeCount : nodeCounts) {
            for (int n = 0; n < nodeCount; n++) {
                cleanStream("rpc:route:" + (baseServerId + n));
            }
            warmUp(baseServerId);

            int msgsPerNode = totalMessages / nodeCount;
            long start = System.nanoTime();
            for (int i = 0; i < totalMessages; i++) {
                int targetServerId = baseServerId + (i % nodeCount);
                redisRoutePublisher.publishRaw(targetServerId, payload);
            }
            long elapsedNs = System.nanoTime() - start;
            long elapsedMs = elapsedNs / 1_000_000;
            double totalOpsPerSec = totalMessages * 1_000_000_000.0 / elapsedNs;
            long avgUs = elapsedNs / totalMessages / 1_000;

            System.out.printf("%-10d %-14d %-14d %-14.0f %-12d%n",
                    nodeCount, msgsPerNode, elapsedMs, totalOpsPerSec, avgUs);

            for (int n = 0; n < nodeCount; n++) {
                Long len = routeRedisTemplate.opsForStream().size("rpc:route:" + (baseServerId + n));
                assertThat(len).as("节点 %d Stream 应有记录", baseServerId + n)
                        .isGreaterThanOrEqualTo(1L);
            }
        }
        System.out.println();
    }

    @Test
    @Order(4)
    @DisplayName("场景二补充：多节点并发写入 — 8/50/100/200/500 线程各写自己的目标节点")
    void multiNodeConcurrentWrite() throws Exception {
        int[] nodeCountList = {8, 50, 100, 200, 500};
        int msgsPerNode = 100;
        byte[] payload = buildPayload(1024);

        System.out.println();
        System.out.println("=== 场景二补充：多节点并发写入 (各 " + msgsPerNode + " 条, 1KB) ===");
        System.out.printf("%-10s %-12s %-14s %-14s %-14s %-14s%n",
                "Nodes", "Total", "Wall(ms)", "Agg Ops/s", "NodeAvg(ms)", "NodeAvg Ops/s");
        System.out.println("-".repeat(80));

        for (int nodeCount : nodeCountList) {
            int baseServerId = 2000 + nodeCount;
            for (int n = 0; n < nodeCount; n++) {
                cleanStream("rpc:route:" + (baseServerId + n));
            }

            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(nodeCount);
            long[] perNodeElapsedMs = new long[nodeCount];

            for (int n = 0; n < nodeCount; n++) {
                final int nodeIdx = n;
                final int targetId = baseServerId + n;
                Thread.ofVirtual().name("writer-" + nodeCount + "-" + n).start(() -> {
                    try {
                        startGate.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    long t0 = System.nanoTime();
                    for (int i = 0; i < msgsPerNode; i++) {
                        redisRoutePublisher.publishRaw(targetId, payload);
                    }
                    perNodeElapsedMs[nodeIdx] = (System.nanoTime() - t0) / 1_000_000;
                    done.countDown();
                });
            }

            long wallStart = System.nanoTime();
            startGate.countDown();
            assertThat(done.await(120, TimeUnit.SECONDS)).isTrue();
            long wallMs = (System.nanoTime() - wallStart) / 1_000_000;

            int totalMsgs = nodeCount * msgsPerNode;
            double wallOps = totalMsgs * 1000.0 / Math.max(1, wallMs);
            long nodeAvgMs = Arrays.stream(perNodeElapsedMs).sum() / nodeCount;
            double nodeAvgOps = msgsPerNode * 1000.0 / Math.max(1, nodeAvgMs);

            System.out.printf("%-10d %-12d %-14d %-14.0f %-14d %-14.0f%n",
                    nodeCount, totalMsgs, wallMs, wallOps, nodeAvgMs, nodeAvgOps);
        }
        System.out.println();
    }

    // ======================== 场景三：双向互写 ========================

    @Test
    @Order(5)
    @DisplayName("场景三：双向互写 — A→B 与 B→A 同时写入的吞吐")
    void bidirectionalWriteThroughput() throws Exception {
        int serverA = 400;
        int serverB = 401;
        int messagesPerSide = 2000;
        byte[] payload = buildPayload(1024);

        cleanStream("rpc:route:" + serverA);
        cleanStream("rpc:route:" + serverB);
        warmUp(serverA);
        warmUp(serverB);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        long[] sideElapsedMs = new long[2];

        Thread.ofVirtual().name("A-to-B").start(() -> {
            try { startGate.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            long t0 = System.nanoTime();
            for (int i = 0; i < messagesPerSide; i++) {
                redisRoutePublisher.publishRaw(serverB, payload);
            }
            sideElapsedMs[0] = (System.nanoTime() - t0) / 1_000_000;
            done.countDown();
        });

        Thread.ofVirtual().name("B-to-A").start(() -> {
            try { startGate.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            long t0 = System.nanoTime();
            for (int i = 0; i < messagesPerSide; i++) {
                redisRoutePublisher.publishRaw(serverA, payload);
            }
            sideElapsedMs[1] = (System.nanoTime() - t0) / 1_000_000;
            done.countDown();
        });

        long wallStart = System.nanoTime();
        startGate.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        long wallMs = (System.nanoTime() - wallStart) / 1_000_000;

        int totalMsgs = messagesPerSide * 2;
        double wallOps = totalMsgs * 1000.0 / Math.max(1, wallMs);

        System.out.println();
        System.out.println("=== 场景三：双向互写 (A↔B, 各 " + messagesPerSide + " 条, 1KB) ===");
        System.out.printf("  总消息: %d | 墙钟耗时: %d ms | 聚合吞吐: %.0f ops/s%n", totalMsgs, wallMs, wallOps);
        System.out.printf("  A→B: %d ms (%.0f ops/s)%n", sideElapsedMs[0], messagesPerSide * 1000.0 / Math.max(1, sideElapsedMs[0]));
        System.out.printf("  B→A: %d ms (%.0f ops/s)%n", sideElapsedMs[1], messagesPerSide * 1000.0 / Math.max(1, sideElapsedMs[1]));

        Long lenA = routeRedisTemplate.opsForStream().size("rpc:route:" + serverA);
        Long lenB = routeRedisTemplate.opsForStream().size("rpc:route:" + serverB);
        System.out.printf("  Stream A 长度: %d | Stream B 长度: %d%n", lenA, lenB);
        System.out.println();

        assertThat(lenA).isGreaterThanOrEqualTo((long) messagesPerSide);
        assertThat(lenB).isGreaterThanOrEqualTo((long) messagesPerSide);
    }

    @Test
    @Order(6)
    @DisplayName("场景三补充：双向互写 + 消费 — 写入同时消费者读取的端到端吞吐")
    void bidirectionalWriteAndConsumeThroughput() throws Exception {
        int serverA = 500;
        int serverB = 501;
        int messagesPerSide = 1000;
        byte[] payload = buildPayload(1024);

        String streamA = "rpc:route:" + serverA;
        String streamB = "rpc:route:" + serverB;
        cleanStream(streamA);
        cleanStream(streamB);

        String group = "perf-test-group";
        String consumerA = "consumer-A";
        String consumerB = "consumer-B";
        ensureGroupExists(streamA, group);
        ensureGroupExists(streamB, group);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch writersDone = new CountDownLatch(2);
        AtomicLong consumedByA = new AtomicLong(0);
        AtomicLong consumedByB = new AtomicLong(0);
        CountDownLatch consumersDone = new CountDownLatch(2);

        Thread.ofVirtual().name("writer-A-to-B").start(() -> {
            try { startGate.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            for (int i = 0; i < messagesPerSide; i++) {
                redisRoutePublisher.publishRaw(serverB, payload);
            }
            writersDone.countDown();
        });

        Thread.ofVirtual().name("writer-B-to-A").start(() -> {
            try { startGate.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            for (int i = 0; i < messagesPerSide; i++) {
                redisRoutePublisher.publishRaw(serverA, payload);
            }
            writersDone.countDown();
        });

        @SuppressWarnings("unchecked")
        StreamOperations<String, String, byte[]> streamOps = routeRedisTemplate.opsForStream();

        Thread.ofVirtual().name("consumer-A").start(() -> {
            try { startGate.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            consumeUntil(streamOps, streamA, group, consumerA, messagesPerSide, consumedByA);
            consumersDone.countDown();
        });

        Thread.ofVirtual().name("consumer-B").start(() -> {
            try { startGate.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            consumeUntil(streamOps, streamB, group, consumerB, messagesPerSide, consumedByB);
            consumersDone.countDown();
        });

        long wallStart = System.nanoTime();
        startGate.countDown();
        assertThat(writersDone.await(60, TimeUnit.SECONDS)).as("写入应在 60s 内完成").isTrue();
        assertThat(consumersDone.await(60, TimeUnit.SECONDS)).as("消费应在 60s 内完成").isTrue();
        long wallMs = (System.nanoTime() - wallStart) / 1_000_000;

        int totalMsgs = messagesPerSide * 2;

        System.out.println();
        System.out.println("=== 场景三补充：双向互写 + 消费 (A↔B, 各 " + messagesPerSide + " 条, 1KB) ===");
        System.out.printf("  总消息: %d | 端到端墙钟: %d ms | 聚合吞吐: %.0f ops/s%n",
                totalMsgs, wallMs, totalMsgs * 1000.0 / Math.max(1, wallMs));
        System.out.printf("  A 消费: %d 条 | B 消费: %d 条%n", consumedByA.get(), consumedByB.get());
        System.out.println();

        assertThat(consumedByA.get()).isEqualTo(messagesPerSide);
        assertThat(consumedByB.get()).isEqualTo(messagesPerSide);
    }

    // ======================== 辅助方法 ========================

    private void consumeUntil(StreamOperations<String, String, byte[]> streamOps,
                              String streamKey, String group, String consumer,
                              int expectedCount, AtomicLong counter) {
        StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(50)
                .block(Duration.ofSeconds(2));
        long deadline = System.currentTimeMillis() + 55_000;

        while (counter.get() < expectedCount && System.currentTimeMillis() < deadline) {
            try {
                List<MapRecord<String, String, byte[]>> records = streamOps.read(
                        Consumer.from(group, consumer),
                        readOptions,
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                );
                if (records != null) {
                    for (MapRecord<String, String, byte[]> record : records) {
                        streamOps.acknowledge(streamKey, group, record.getId());
                        streamOps.delete(streamKey, record.getId());
                        counter.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    private void ensureGroupExists(String streamKey, String group) {
        try {
            byte[] rawKey = streamKey.getBytes(StandardCharsets.UTF_8);
            routeRedisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.streamCommands().xGroupCreate(rawKey, group, ReadOffset.from("0"), true);
                return null;
            });
        } catch (Exception ignored) {
        }
    }

    private static String formatSize(int bytes) {
        if (bytes < 1024) return bytes + "B";
        return (bytes / 1024) + "KB";
    }
}
