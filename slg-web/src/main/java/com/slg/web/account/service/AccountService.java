package com.slg.web.account.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.entity.mysql.datatype.DataList;
import com.slg.web.account.entity.AccountEntity;
import com.slg.web.account.model.RoleBriefInfo;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 账号服务
 * 提供 Account 的创建、查询、更新等操作
 * Account 是账号体系的聚合根，roleInfoList 由 game 侧创角回调写入并维护
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

    /**
     * 添加角色简要信息到账号的角色列表
     * 在 game 侧创角回调中调用，幂等（已存在则更新 serverId）
     *
     * @param account  账号实体
     * @param roleId   角色 ID
     * @param serverId 角色所在 game 服 ID
     */
    public void addRoleInfo(AccountEntity account, long roleId, long serverId) {
        DataList<RoleBriefInfo> list = account.getRoleInfoList();
        boolean found = false;
        for (RoleBriefInfo info : list.getList()) {
            if (info.getRoleId() == roleId) {
                info.setServerId(serverId);
                found = true;
                break;
            }
        }
        if (!found) {
            list.getList().add(RoleBriefInfo.of(roleId, serverId));
        }
        accountCache.saveField(account, "roleInfoList");
    }

    /**
     * 从账号角色列表中移除指定角色（删角/账号注销时调用）
     *
     * @param account 账号实体
     * @param roleId  要移除的角色 ID
     */
    public void removeRoleInfo(AccountEntity account, long roleId) {
        DataList<RoleBriefInfo> list= account.getRoleInfoList();
        boolean removed = list.getList().removeIf(info -> info.getRoleId() == roleId);
        if (removed) {
            if (account.getMainRoleId() == roleId) {
                long newMain = list.getList().isEmpty() ? 0L : list.getList().getFirst().getRoleId();
                account.setMainRoleId(newMain);
            }
            accountCache.save(account);
        }
    }

    /**
     * 查找账号中指定 roleId 对应的角色简要信息
     *
     * @param account 账号实体
     * @param roleId  角色 ID
     * @return 对应的 RoleBriefInfo，不存在返回 null
     */
    public RoleBriefInfo findRoleInfo(AccountEntity account, long roleId) {
        for (RoleBriefInfo info : account.getRoleInfoList().getList()) {
            if (info.getRoleId() == roleId) {
                return info;
            }
        }
        return null;
    }
}
