package com.slg.log.alert.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.CountResponse;
import com.slg.log.alert.entity.AlertRuleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 告警定时检测服务
 * 每 60 秒检查一次所有已启用的告警规则，查询 ES 判断是否达到阈值
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCheckService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ElasticsearchClient esClient;
    private final AlertRuleService alertRuleService;
    private final AlertRecordService alertRecordService;
    private final DingTalkNotifyService dingTalkNotifyService;

    @Value("${elasticsearch.index-prefix}")
    private String indexPrefix;

    /**
     * 定时检测所有启用的告警规则
     */
    @Scheduled(fixedDelay = 60000)
    public void checkAlertRules() {
        List<AlertRuleEntity> enabledRules;
        try {
            enabledRules = alertRuleService.listEnabled();
        } catch (Exception e) {
            log.error("[AlertCheck] 获取告警规则失败", e);
            return;
        }

        if (enabledRules.isEmpty()) {
            return;
        }

        for (AlertRuleEntity rule : enabledRules) {
            try {
                checkSingleRule(rule);
            } catch (Exception e) {
                log.error("[AlertCheck] 检测规则 [{}] 异常", rule.getName(), e);
            }
        }
    }

    private void checkSingleRule(AlertRuleEntity rule) throws Exception {
        if (isInCooldown(rule)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(rule.getTimeWindowMinutes());

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        boolBuilder.filter(f -> f.range(r -> r.date(d -> d
                .field("@timestamp")
                .gte(windowStart.format(ISO_FMT))
                .lte(now.format(ISO_FMT))
        )));

        if (StringUtils.hasText(rule.getLevel())) {
            boolBuilder.filter(f -> f.term(t -> t.field("level").value(rule.getLevel())));
        }

        if (StringUtils.hasText(rule.getServerId())) {
            boolBuilder.filter(f -> f.term(t -> t.field("server_id").value(rule.getServerId())));
        }

        if (StringUtils.hasText(rule.getKeyword())) {
            boolBuilder.must(q -> q.match(m -> m.field("message").query(rule.getKeyword())));
        }

        CountResponse countResp = esClient.count(c -> c
                .index(indexPrefix + "-*")
                .query(q -> q.bool(boolBuilder.build()))
        );

        long matchCount = countResp.count();

        if (matchCount >= rule.getThreshold()) {
            triggerAlert(rule, matchCount);
        }
    }

    private boolean isInCooldown(AlertRuleEntity rule) {
        if (rule.getLastTriggeredTime() == null) {
            return false;
        }
        LocalDateTime cooldownEnd = rule.getLastTriggeredTime().plusMinutes(rule.getCooldownMinutes());
        return LocalDateTime.now().isBefore(cooldownEnd);
    }

    private void triggerAlert(AlertRuleEntity rule, long matchCount) {
        log.debug("[AlertCheck] 触发告警: rule={}, matchCount={}, threshold={}",
                rule.getName(), matchCount, rule.getThreshold());

        String notifyResult = "SKIPPED";
        String errorMsg = null;

        if (StringUtils.hasText(rule.getWebhookUrl())) {
            notifyResult = dingTalkNotifyService.sendAlert(
                    rule.getWebhookUrl(),
                    rule.getName(),
                    matchCount,
                    rule.getLevel(),
                    rule.getKeyword(),
                    rule.getServerId(),
                    rule.getTimeWindowMinutes()
            );
            if (!"SUCCESS".equals(notifyResult)) {
                errorMsg = notifyResult;
                notifyResult = "FAILED";
            }
        }

        String notifyMessage = String.format("规则[%s]触发: %d条日志匹配(阈值%d), 时间窗口%d分钟",
                rule.getName(), matchCount, rule.getThreshold(), rule.getTimeWindowMinutes());

        alertRecordService.createRecord(
                rule.getId(),
                rule.getName(),
                matchCount,
                notifyResult,
                notifyMessage,
                errorMsg
        );

        rule.setLastTriggeredTime(LocalDateTime.now());
        alertRuleService.save(rule);
    }
}
