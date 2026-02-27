package com.slg.log.alert.dto;

import lombok.Data;

import java.util.List;

/**
 * 告警记录分页响应 DTO
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
public class AlertRecordResponse {

    private long total;
    private List<AlertRecordItem> records;

    public AlertRecordResponse(long total, List<AlertRecordItem> records) {
        this.total = total;
        this.records = records;
    }

    @Data
    public static class AlertRecordItem {
        private Long id;
        private Long ruleId;
        private String ruleName;
        private Long matchCount;
        private String triggerTime;
        private String notifyStatus;
        private String notifyMessage;
        private String errorMessage;
    }
}
