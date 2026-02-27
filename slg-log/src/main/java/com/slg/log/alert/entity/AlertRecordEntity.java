package com.slg.log.alert.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.mysql.entity.BaseMysqlEntity;
import com.slg.log.alert.service.AlertRecordService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 告警记录实体
 * 记录每次告警触发的详细信息和通知结果
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "alert_record")
@CacheConfig(maxSize = 500, expireMinutes = 1440)
public class AlertRecordEntity extends BaseMysqlEntity<Long> {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Override
    public Long getId() {
        return id;
    }

    /** 关联规则 ID */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /** 规则名称（冗余存储方便查看） */
    @Column(name = "rule_name", length = 100)
    private String ruleName;

    /** 匹配到的日志数量 */
    @Column(name = "match_count")
    private Long matchCount;

    /** 触发时间 */
    @Column(name = "trigger_time", nullable = false)
    private LocalDateTime triggerTime;

    /** 通知状态（SUCCESS / FAILED） */
    @Column(name = "notify_status", length = 20)
    private String notifyStatus;

    /** 通知内容摘要 */
    @Column(name = "notify_message", length = 1000)
    private String notifyMessage;

    /** 失败原因 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Override
    public void save() {
        AlertRecordService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        AlertRecordService.getInstance().saveField(this, fieldName);
    }
}
