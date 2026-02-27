package com.slg.log.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 日志搜索响应
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogSearchResponse {

    private long total;
    private List<LogEntry> entries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        private String timestamp;
        private String level;
        private String loggerName;
        private String threadName;
        private String message;
        private String stackTrace;
        private String serverId;
        private String serverType;
        /** 高亮片段 */
        private Map<String, List<String>> highlights;
    }
}
