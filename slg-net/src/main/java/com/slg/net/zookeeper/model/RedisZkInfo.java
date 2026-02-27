package com.slg.net.zookeeper.model;

import lombok.Getter;
import lombok.Setter;

/**
 * ZK 中存储的 Redis 连接信息
 * 对应节点树：{serverPath}/Redis/{host, port, password}
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Getter
@Setter
public class RedisZkInfo {

    /** Redis 主机地址 */
    private String host;

    /** Redis 端口 */
    private int port;

    /** Redis 密码（无密码时为空字符串或 null） */
    private String password;
}
