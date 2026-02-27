package com.slg.log.stats.dto;

import lombok.Data;

/**
 * 统计请求参数
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
public class LogStatsRequest {

    /** 起始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 服务器ID过滤 */
    private String serverId;

    /** 日志级别过滤 */
    private String level;
}
