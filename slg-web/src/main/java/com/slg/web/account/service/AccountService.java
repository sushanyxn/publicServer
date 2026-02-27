package com.slg.web.account.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.web.account.entity.AccountEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 账号服务
 * 提供 Account 的创建、查询、更新等操作
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Service
public class AccountService {

    @Getter
    private static AccountService instance;

    @EntityCacheInject
    private EntityCache<AccountEntity> accountCache;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * 全量加载账号数据到缓存
     */
    public void loadAll() {
        accountCache.loadAll();
    }

    /**
     * 根据 ID 查找账号
     */
    public AccountEntity findById(long id) {
        return accountCache.findById(id);
    }

    /**
     * 创建新账号
     *
     * @param advertisingId 广告 ID（GAID）
     * @return 新创建的 AccountEntity
     */
    public AccountEntity createAccount(String advertisingId) {
        AccountEntity account = new AccountEntity();
        account.setAdvertisingId(advertisingId);
        account.setCreateTime(LocalDateTime.now());
        return accountCache.insert(account);
    }

    /**
     * 保存（更新）账号
     */
    public AccountEntity save(AccountEntity account) {
        account.setUpdateTime(LocalDateTime.now());
        return accountCache.save(account);
    }

    /**
     * 保存单个字段
     */
    public void saveField(AccountEntity account, String fieldName) {
        accountCache.saveField(account, fieldName);
    }
}
