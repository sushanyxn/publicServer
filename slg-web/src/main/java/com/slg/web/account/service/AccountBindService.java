package com.slg.web.account.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.web.account.entity.AccountBindEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账号绑定服务
 * 提供 AccountBind 的创建、查询、更新等操作
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
     * 根据平台 ID 和平台类型查找绑定记录
     *
     * @param platformId 平台唯一标识
     * @param platform   平台类型 {@link com.slg.common.constant.PlatformType}
     * @return 绑定实体，不存在返回 null
     */
    public AccountBindEntity findByPlatformIdAndPlatform(String platformId, int platform) {
        return accountBindCache.getAllCache().stream()
                .filter(bind -> bind.getPlatformId().equals(platformId) && bind.getPlatform() == platform)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 Account ID 查找所有绑定记录
     */
    public List<AccountBindEntity> findByAccId(long accId) {
        return accountBindCache.getAllCache().stream()
                .filter(bind -> bind.getAccId() == accId)
                .toList();
    }

    /**
     * 根据平台 ID 查找所有绑定记录（跨平台类型）
     */
    public List<AccountBindEntity> findByPlatformId(String platformId) {
        return accountBindCache.getAllCache().stream()
                .filter(bind -> bind.getPlatformId().equals(platformId))
                .toList();
    }

    /**
     * 创建新的绑定关系
     *
     * @param platformId 平台唯一 ID
     * @param platform   平台类型
     * @param accId      关联的 Account ID
     * @return 新创建的绑定实体
     */
    public AccountBindEntity createAccountBind(String platformId, int platform, long accId) {
        AccountBindEntity bind = new AccountBindEntity();
        bind.setPlatformId(platformId);
        bind.setPlatform(platform);
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
     */
    public void delete(long id) {
        accountBindCache.evict(id);
    }
}
