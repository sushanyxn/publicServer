package com.slg.log.alert.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.mysql.entity.BaseMysqlEntity;
import com.slg.log.alert.service.AlertRuleService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 告警规则实体
 * 定义日志告警的条件、阈值和通知方式
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "alert_rule")
@CacheConfig(maxSize = -1, expireMinutes = -1)
public class AlertRuleEntity extends BaseMysqlEntity<Long> {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Override
    public Long getId() {
        return id;
    }

    /** 规则名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 监控日志级别（ERROR/WARN/空=全部） */
    @Column(length = 20)
    private String level;

    /** 关键词匹配（可空，匹配 message 字段） */
    @Column(length = 200)
    private String keyword;

    /** 限定服务器ID（可空=全部） */
    @Column(name = "server_id", length = 50)
    private String serverId;

    /** 触发阈值（时间窗口内达到此数量触发） */
    @Column(nullable = false)
    private Integer threshold;

    /** 检测时间窗口（分钟） */
    @Column(name = "time_window_minutes", nullable = false)
    private Integer timeWindowMinutes;

    /** 冷却时间（分钟），同一规则触发后多久内不重复告警 */
    @Column(name = "cooldown_minutes", nullable = false)
    private Integer cooldownMinutes;

    /** 钉钉 Webhook 地址 */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    /** 上次触发时间 */
    @Column(name = "last_triggered_time")
    private LocalDateTime lastTriggeredTime;

    @Override
    public void save() {
        AlertRuleService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        AlertRuleService.getInstance().saveField(this, fieldName);
    }
}
