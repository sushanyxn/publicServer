package com.slg.net.zookeeper.model;

import lombok.Getter;
import lombok.Setter;

/**
 * ZK 中存储的 MongoDB 连接信息
 * 对应节点树：{serverPath}/MongoDB/{db_name, url}
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Getter
@Setter
public class MongoZkInfo {

    /** 数据库名称 */
    private String dbName;

    /** MongoDB 连接 URL */
    private String url;
}
