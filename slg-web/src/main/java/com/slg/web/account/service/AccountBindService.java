package com.slg.web.account.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.web.account.entity.AccountBindEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 账号绑定服务
 * 提供 AccountBind 的创建、查询、更新等操作
 * 主键为 {platform}_{platformId}，查询直接走主键索引，O(1)
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Service
public class AccountBindService {

    @Getter
    private static AccountBindService instance;

    @EntityCacheInject
    private EntityCache<AccountBindEntity> accountBindCache;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * 全量加载绑定数据到缓存
     */
    public void loadAll() {
        accountBindCache.loadAll();
    }

    /**
     * 根据平台 ID 和平台类型查找绑定记录（主键查询，O(1)）
     *
     * @param platformId 平台唯一标识（不可含 _）
     * @param platform   平台类型 {@link com.slg.common.constant.PlatformType}
     * @return 绑定实体，不存在返回 null
     */
    public AccountBindEntity findByPlatformAndId(int platform, String platformId) {
        return accountBindCache.findById(AccountBindEntity.buildKey(platform, platformId));
    }

    /**
     * 创建新的绑定关系
     * 主键由 platform + "_" + platformId 拼接，platformId 中不可含 _
     *
     * @param platform   平台类型
     * @param platformId 平台唯一 ID（不可含 _）
     * @param accId      关联的 Account ID
     * @return 新创建的绑定实体
     */
    public AccountBindEntity createAccountBind(int platform, String platformId, long accId) {
        AccountBindEntity bind = new AccountBindEntity();
        bind.setId(AccountBindEntity.buildKey(platform, platformId));
        bind.setPlatform(platform);
        bind.setPlatformId(platformId);
        bind.setAccId(accId);
        bind.setBindTime(LocalDateTime.now());
        bind.setCreateTime(LocalDateTime.now());
        return accountBindCache.insert(bind);
    }

    /**
     * 保存（更新）绑定记录
     */
    public AccountBindEntity save(AccountBindEntity bind) {
        return accountBindCache.save(bind);
    }

    /**
     * 保存单个字段
     */
    public void saveField(AccountBindEntity bind, String fieldName) {
        accountBindCache.saveField(bind, fieldName);
    }

    /**
     * 删除绑定记录
     *
     * @param platform   平台类型
     * @param platformId 平台唯一 ID
     */
    public void delete(int platform, String platformId) {
        accountBindCache.evict(AccountBindEntity.buildKey(platform, platformId));
    }
}
