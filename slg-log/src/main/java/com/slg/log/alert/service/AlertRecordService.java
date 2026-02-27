package com.slg.log.alert.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.log.alert.dto.AlertRecordResponse;
import com.slg.log.alert.dto.AlertRecordResponse.AlertRecordItem;
import com.slg.log.alert.entity.AlertRecordEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 告警记录服务
 * 提供告警记录的创建和查询功能
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Service
public class AlertRecordService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Getter
    private static AlertRecordService instance;

    @EntityCacheInject
    private EntityCache<AlertRecordEntity> recordCache;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * 全量加载记录到缓存
     */
    public void loadAll() {
        recordCache.loadAll();
    }

    /**
     * 创建告警记录
     */
    public AlertRecordEntity createRecord(Long ruleId, String ruleName, long matchCount,
                                          String notifyStatus, String notifyMessage, String errorMessage) {
        AlertRecordEntity record = new AlertRecordEntity();
        record.setRuleId(ruleId);
        record.setRuleName(ruleName);
        record.setMatchCount(matchCount);
        record.setTriggerTime(LocalDateTime.now());
        record.setNotifyStatus(notifyStatus);
        record.setNotifyMessage(notifyMessage);
        record.setErrorMessage(errorMessage);
        return recordCache.insert(record);
    }

    /**
     * 分页查询告警记录
     *
     * @param ruleId 规则ID过滤（可空）
     * @param page   页码（0-based）
     * @param size   每页大小
     */
    public AlertRecordResponse queryRecords(Long ruleId, int page, int size) {
        Stream<AlertRecordEntity> stream = recordCache.getAllCache().stream();

        if (ruleId != null) {
            stream = stream.filter(r -> ruleId.equals(r.getRuleId()));
        }

        List<AlertRecordEntity> sorted = stream
                .sorted(Comparator.comparing(AlertRecordEntity::getTriggerTime).reversed())
                .collect(Collectors.toList());

        long total = sorted.size();
        int fromIndex = Math.min(page * size, sorted.size());
        int toIndex = Math.min(fromIndex + size, sorted.size());
        List<AlertRecordEntity> pageData = sorted.subList(fromIndex, toIndex);

        List<AlertRecordItem> items = pageData.stream().map(r -> {
            AlertRecordItem item = new AlertRecordItem();
            item.setId(r.getId());
            item.setRuleId(r.getRuleId());
            item.setRuleName(r.getRuleName());
            item.setMatchCount(r.getMatchCount());
            item.setTriggerTime(r.getTriggerTime() != null ? r.getTriggerTime().format(FMT) : null);
            item.setNotifyStatus(r.getNotifyStatus());
            item.setNotifyMessage(r.getNotifyMessage());
            item.setErrorMessage(r.getErrorMessage());
            return item;
        }).collect(Collectors.toList());

        return new AlertRecordResponse(total, items);
    }

    /**
     * 保存记录实体
     */
    public AlertRecordEntity save(AlertRecordEntity entity) {
        return recordCache.save(entity);
    }

    /**
     * 保存记录单个字段
     */
    public void saveField(AlertRecordEntity entity, String fieldName) {
        recordCache.saveField(entity, fieldName);
    }
}
