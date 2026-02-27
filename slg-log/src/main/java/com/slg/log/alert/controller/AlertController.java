package com.slg.log.alert.controller;

import com.slg.log.alert.dto.AlertRecordResponse;
import com.slg.log.alert.dto.AlertRuleRequest;
import com.slg.log.alert.entity.AlertRuleEntity;
import com.slg.log.alert.service.AlertRecordService;
import com.slg.log.alert.service.AlertRuleService;
import com.slg.log.alert.service.DingTalkNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警管理接口
 * 提供告警规则的 CRUD、告警记录查询、钉钉推送测试
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleService alertRuleService;
    private final AlertRecordService alertRecordService;
    private final DingTalkNotifyService dingTalkNotifyService;

    /**
     * 获取所有告警规则
     */
    @GetMapping("/rules")
    public List<AlertRuleEntity> listRules() {
        return alertRuleService.listAll();
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rules")
    public AlertRuleEntity createRule(@RequestBody AlertRuleRequest request) {
        return alertRuleService.createRule(request);
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/rules/{id}")
    public AlertRuleEntity updateRule(@PathVariable Long id, @RequestBody AlertRuleRequest request) {
        return alertRuleService.updateRule(id, request);
    }

    /**
     * 启用/禁用告警规则
     */
    @PutMapping("/rules/{id}/enabled")
    public Map<String, Object> toggleRule(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        alertRuleService.setEnabled(id, enabled);
        return Map.of("success", true);
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/rules/{id}")
    public Map<String, Object> deleteRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return Map.of("success", true);
    }

    /**
     * 分页查询告警记录
     *
     * @param ruleId 规则ID（可选）
     * @param page   页码（0-based）
     * @param size   每页大小
     */
    @GetMapping("/records")
    public AlertRecordResponse listRecords(
            @RequestParam(required = false) Long ruleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return alertRecordService.queryRecords(ruleId, page, size);
    }

    /**
     * 测试钉钉推送
     */
    @PostMapping("/test-notify")
    public Map<String, Object> testNotify(@RequestBody Map<String, String> body) {
        String webhookUrl = body.get("webhookUrl");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("webhookUrl 不能为空");
        }
        String result = dingTalkNotifyService.sendTestMessage(webhookUrl);
        boolean success = "SUCCESS".equals(result);
        return Map.of("success", success, "message", result);
    }
}
