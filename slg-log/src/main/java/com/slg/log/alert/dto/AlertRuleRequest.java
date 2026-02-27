package com.slg.log.alert.dto;

import lombok.Data;

/**
 * 告警规则请求 DTO
 * 用于创建和更新告警规则
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
public class AlertRuleRequest {

    /** 规则名称 */
    private String name;

    /** 是否启用 */
    private Boolean enabled;

    /** 监控日志级别 */
    private String level;

    /** 关键词匹配 */
    private String keyword;

    /** 限定服务器ID */
    private String serverId;

    /** 触发阈值 */
    private Integer threshold;

    /** 检测时间窗口（分钟） */
    private Integer timeWindowMinutes;

    /** 冷却时间（分钟） */
    private Integer cooldownMinutes;

    /** 钉钉 Webhook 地址 */
    private String webhookUrl;
}
