package com.slg.log.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统计响应
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogStatsResponse {

    /** 总日志数 */
    private long totalCount;

    /** 按天统计 */
    private List<BucketCount> byDay;

    /** 按服务器统计 */
    private List<BucketCount> byServer;

    /** 按级别统计 */
    private List<BucketCount> byLevel;

    /** 按天+服务器交叉统计 */
    private List<DayServerCount> byDayAndServer;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BucketCount {
        private String key;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayServerCount {
        private String day;
        private String serverId;
        private long count;
    }
}
