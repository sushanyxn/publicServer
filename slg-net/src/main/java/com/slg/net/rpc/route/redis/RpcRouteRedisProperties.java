package com.slg.net.rpc.route.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RPC Redis 路由转发专用配置属性
 * 对应 application.yml 中 rpc.route.redis.* 配置项
 * 与业务 Redis（spring.data.redis.*）完全隔离
 *
 * @author yangxunan
 * @date 2026/03/04
 */
@ConfigurationProperties(prefix = "rpc.route.redis")
public class RpcRouteRedisProperties {

    /** 转发 Redis 主机地址 */
    private String host = "localhost";

    /** 转发 Redis 端口（默认 6380，区分业务 Redis 6379） */
    private int port = 6380;

    /** 转发 Redis 密码（可为空） */
    private String password;

    /** 转发 Redis 数据库编号 */
    private int database = 0;

    /** 连接超时（毫秒） */
    private int timeout = 3000;

    /** 消费者组名称 */
    private String consumerGroup = "rpc-route-group";

    /** 每次批量读取的最大消息条数 */
    private int batchSize = 10;

    /** 阻塞读取超时（秒），用于控制循环间隔与优雅停机响应速度 */
    private int blockSeconds = 1;

    /** Stream 最大长度，XADD 时近似裁剪，防止目标服务器离线时 Stream 无限膨胀 */
    private long streamMaxLen = 10000;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getDatabase() { return database; }
    public void setDatabase(int database) { this.database = database; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public int getBlockSeconds() { return blockSeconds; }
    public void setBlockSeconds(int blockSeconds) { this.blockSeconds = blockSeconds; }

    public long getStreamMaxLen() { return streamMaxLen; }
    public void setStreamMaxLen(long streamMaxLen) { this.streamMaxLen = streamMaxLen; }
}
