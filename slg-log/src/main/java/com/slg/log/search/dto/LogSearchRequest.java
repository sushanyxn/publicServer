package com.slg.log.search.dto;

import lombok.Data;

/**
 * 日志搜索请求参数
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
public class LogSearchRequest {

    /** 搜索关键词，多个用空格分隔，~前缀表示排除 */
    private String query;

    /** 起始时间（ISO 格式，如 2026-02-01T00:00:00） */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 服务器ID过滤（为空表示不过滤） */
    private String serverId;

    /** 服务器类型过滤：game / scene / singlestart */
    private String serverType;

    /** 日志级别过滤：WARN / ERROR 等 */
    private String level;

    /** 页码（从 0 开始） */
    private int page = 0;

    /** 每页条数 */
    private int size = 50;
}
