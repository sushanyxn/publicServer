package com.slg.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Logback Appender，将日志事件异步批量推送到 Elasticsearch
 * <p>
 * 设计要点：
 * <ul>
 *   <li>业务线程只做 offer 入队（纳秒级），JSON 序列化推迟到 flusher 守护线程</li>
 *   <li>flusher 每次 flush 循环消费队列直至清空或达到上限，避免堆积</li>
 *   <li>队列满时直接丢弃，永不阻塞业务线程</li>
 *   <li>队列使用率超过 80% 时对非 ERROR 日志按比例采样，优先保留关键日志</li>
 *   <li>Semaphore 限制同时在飞的 HTTP 请求数，防止 ES 慢响应导致请求堆积</li>
 *   <li>堆栈深度可配置截断，减少序列化开销和 ES 存储压力</li>
 * </ul>
 * <p>
 * 通过 logback XML 配置属性：
 * <ul>
 *   <li>enabled - 是否启用（false 时完全不初始化，零开销）</li>
 *   <li>esHost / esPort / esScheme - ES 连接信息</li>
 *   <li>indexPrefix - 索引前缀，实际索引为 {prefix}-{yyyy.MM.dd}</li>
 *   <li>serverId / serverType - 注入到每条日志的服务器标识</li>
 *   <li>batchSize - 每次 bulk 请求的最大条数（默认 200）</li>
 *   <li>flushIntervalMs - 定时刷新间隔毫秒（默认 1000）</li>
 *   <li>bufferCapacity - 内存缓冲队列容量（默认 10000）</li>
 *   <li>maxStackLines - 单个异常链最大堆栈行数（默认 50）</li>
 *   <li>maxInflightRequests - 同时在飞的 HTTP 请求上限（默认 5）</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026-02-26
 */
public class ElasticsearchAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private boolean enabled = false;
    private String esHost = "localhost";
    private int esPort = 9200;
    private String esScheme = "http";
    private String indexPrefix = "slg-logs";
    private String serverId = "0";
    private String serverType = "unknown";
    private int batchSize = 200;
    private int flushIntervalMs = 1000;
    private int bufferCapacity = 10000;
    private int maxStackLines = 50;
    private int maxInflightRequests = 5;

    /** 采样启动的队列使用率阈值 */
    private static final double SAMPLE_THRESHOLD = 0.8;
    /** 单次 flush 最多消费的批次数，防止 flusher 长时间独占 */
    private static final int MAX_BATCHES_PER_FLUSH = 20;

    private BlockingQueue<ILoggingEvent> buffer;
    private HttpClient httpClient;
    private ScheduledExecutorService scheduler;
    private Semaphore inflightSemaphore;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter INDEX_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Override
    public void start() {
        super.start();
        if (!enabled) {
            return;
        }

        buffer = new LinkedBlockingQueue<>(bufferCapacity);
        inflightSemaphore = new Semaphore(maxInflightRequests);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "es-log-flusher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::flush, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);

        addInfo("ElasticsearchAppender started, target: " + esScheme + "://" + esHost + ":" + esPort
                + ", batchSize=" + batchSize + ", bufferCapacity=" + bufferCapacity
                + ", maxStackLines=" + maxStackLines + ", maxInflight=" + maxInflightRequests);
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!enabled || buffer == null) {
            return;
        }
        try {
            double usage = (double) buffer.size() / bufferCapacity;
            if (usage > SAMPLE_THRESHOLD) {
                if (!"ERROR".equals(event.getLevel().toString())) {
                    double keepRate = 1.0 - (usage - SAMPLE_THRESHOLD) / (1.0 - SAMPLE_THRESHOLD);
                    if (ThreadLocalRandom.current().nextDouble() > keepRate) {
                        return;
                    }
                }
            }

            event.prepareForDeferredProcessing();
            buffer.offer(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 循环消费队列，每次取一批发送，直到队列空或达到批次上限
     */
    private void flush() {
        if (buffer == null || buffer.isEmpty()) {
            return;
        }

        int batchesSent = 0;
        while (!buffer.isEmpty() && batchesSent < MAX_BATCHES_PER_FLUSH) {
            try {
                List<ILoggingEvent> events = new ArrayList<>(batchSize);
                buffer.drainTo(events, batchSize);
                if (events.isEmpty()) {
                    break;
                }

                String indexName = indexPrefix + "-" + LocalDate.now().format(INDEX_DATE_FORMAT);
                StringBuilder bulk = new StringBuilder(events.size() * 512);
                for (ILoggingEvent event : events) {
                    String json = formatEvent(event);
                    bulk.append("{\"index\":{\"_index\":\"").append(indexName).append("\"}}\n");
                    bulk.append(json).append('\n');
                }

                sendBulk(bulk.toString());
                batchesSent++;
            } catch (Exception e) {
                addError("ES log flush error: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * 通过 Semaphore 控制并发，防止 ES 慢响应时请求无限堆积
     */
    private void sendBulk(String body) {
        if (!inflightSemaphore.tryAcquire()) {
            addWarn("ES inflight requests limit reached, dropping batch");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(esScheme + "://" + esHost + ":" + esPort + "/_bulk"))
                .header("Content-Type", "application/x-ndjson")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((resp, ex) -> {
                    inflightSemaphore.release();
                    if (ex != null) {
                        addError("Failed to send log batch to ES: " + ex.getMessage());
                    }
                });
    }

    private String formatEvent(ILoggingEvent event) {
        try {
            ObjectNode node = OBJECT_MAPPER.createObjectNode();
            node.put("@timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
            node.put("level", event.getLevel().toString());
            node.put("logger_name", event.getLoggerName());
            node.put("thread_name", event.getThreadName());
            node.put("message", event.getFormattedMessage());
            node.put("server_id", serverId);
            node.put("server_type", serverType);

            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                node.put("stack_trace", formatThrowable(throwableProxy));
            }

            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"message\":\"log serialization error\",\"level\":\"ERROR\"}";
        }
    }

    private String formatThrowable(IThrowableProxy proxy) {
        StringBuilder sb = new StringBuilder(512);
        int[] linesWritten = {0};
        buildThrowableString(sb, proxy, false, linesWritten);
        return sb.toString();
    }

    private void buildThrowableString(StringBuilder sb, IThrowableProxy proxy,
                                      boolean isCause, int[] linesWritten) {
        if (proxy == null) {
            return;
        }
        if (isCause) {
            sb.append("Caused by: ");
        }
        sb.append(proxy.getClassName()).append(": ").append(proxy.getMessage()).append('\n');

        StackTraceElementProxy[] frames = proxy.getStackTraceElementProxyArray();
        int remaining = maxStackLines - linesWritten[0];
        int limit = Math.min(frames.length, Math.max(remaining, 0));

        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(frames[i].getSTEAsString()).append('\n');
            linesWritten[0]++;
        }

        if (frames.length > limit) {
            sb.append("\t... ").append(frames.length - limit).append(" more\n");
        }

        if (linesWritten[0] < maxStackLines) {
            buildThrowableString(sb, proxy.getCause(), true, linesWritten);
        } else if (proxy.getCause() != null) {
            sb.append("Caused by: ").append(proxy.getCause().getClassName())
                    .append(": ").append(proxy.getCause().getMessage())
                    .append(" [stack truncated]\n");
        }
    }

    @Override
    public void stop() {
        if (enabled && scheduler != null) {
            flush();
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        super.stop();
    }

    // ========== Logback XML 配置属性的 Setter ==========

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEsHost(String esHost) {
        this.esHost = esHost;
    }

    public void setEsPort(int esPort) {
        this.esPort = esPort;
    }

    public void setEsScheme(String esScheme) {
        this.esScheme = esScheme;
    }

    public void setIndexPrefix(String indexPrefix) {
        this.indexPrefix = indexPrefix;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setFlushIntervalMs(int flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public void setBufferCapacity(int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
    }

    public void setMaxStackLines(int maxStackLines) {
        this.maxStackLines = maxStackLines;
    }

    public void setMaxInflightRequests(int maxInflightRequests) {
        this.maxInflightRequests = maxInflightRequests;
    }
}
