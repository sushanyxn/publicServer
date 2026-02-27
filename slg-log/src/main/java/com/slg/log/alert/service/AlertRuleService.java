package com.slg.log.alert.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.log.alert.dto.AlertRuleRequest;
import com.slg.log.alert.entity.AlertRuleEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 告警规则管理服务
 * 通过 EntityCache 提供规则的 CRUD 操作
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Service
public class AlertRuleService {

    @Getter
    private static AlertRuleService instance;

    @EntityCacheInject
    private EntityCache<AlertRuleEntity> ruleCache;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * 全量加载规则数据到缓存
     */
    public void loadAll() {
        ruleCache.loadAll();
    }

    /**
     * 获取所有规则列表
     */
    public List<AlertRuleEntity> listAll() {
        return new ArrayList<>(ruleCache.getAllCache());
    }

    /**
     * 获取所有已启用的规则
     */
    public List<AlertRuleEntity> listEnabled() {
        return ruleCache.getAllCache().stream()
                .filter(AlertRuleEntity::getEnabled)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 查找规则
     */
    public AlertRuleEntity findById(Long id) {
        return ruleCache.findById(id);
    }

    /**
     * 创建告警规则
     */
    public AlertRuleEntity createRule(AlertRuleRequest request) {
        AlertRuleEntity entity = new AlertRuleEntity();
        entity.setName(request.getName());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        entity.setLevel(request.getLevel());
        entity.setKeyword(request.getKeyword());
        entity.setServerId(request.getServerId());
        entity.setThreshold(request.getThreshold());
        entity.setTimeWindowMinutes(request.getTimeWindowMinutes());
        entity.setCooldownMinutes(request.getCooldownMinutes());
        entity.setWebhookUrl(request.getWebhookUrl());
        return ruleCache.insert(entity);
    }

    /**
     * 更新告警规则
     */
    public AlertRuleEntity updateRule(Long id, AlertRuleRequest request) {
        AlertRuleEntity entity = ruleCache.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("规则不存在: " + id);
        }
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getLevel() != null) {
            entity.setLevel(request.getLevel());
        }
        if (request.getKeyword() != null) {
            entity.setKeyword(request.getKeyword());
        }
        if (request.getServerId() != null) {
            entity.setServerId(request.getServerId());
        }
        if (request.getThreshold() != null) {
            entity.setThreshold(request.getThreshold());
        }
        if (request.getTimeWindowMinutes() != null) {
            entity.setTimeWindowMinutes(request.getTimeWindowMinutes());
        }
        if (request.getCooldownMinutes() != null) {
            entity.setCooldownMinutes(request.getCooldownMinutes());
        }
        if (request.getWebhookUrl() != null) {
            entity.setWebhookUrl(request.getWebhookUrl());
        }
        return ruleCache.save(entity);
    }

    /**
     * 启用/禁用规则
     */
    public void setEnabled(Long id, boolean enabled) {
        AlertRuleEntity entity = ruleCache.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("规则不存在: " + id);
        }
        entity.setEnabled(enabled);
        ruleCache.save(entity);
    }

    /**
     * 删除规则
     */
    public void deleteRule(Long id) {
        ruleCache.deleteById(id);
    }

    /**
     * 保存规则实体
     */
    public AlertRuleEntity save(AlertRuleEntity entity) {
        return ruleCache.save(entity);
    }

    /**
     * 保存规则单个字段
     */
    public void saveField(AlertRuleEntity entity, String fieldName) {
        ruleCache.saveField(entity, fieldName);
    }
}
