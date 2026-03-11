package com.slg.game.base.account.manager;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.game.base.account.entity.AccountEntity;
import com.slg.game.base.account.model.AccountRoleBrief;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 玩家账号管理
 * 管理账号-角色对应关系缓存，提供按角色存在性、最近登录角色等查询
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Component
@Getter
public class AccountManager {

    private static AccountManager instance;

    @EntityCacheInject
    private EntityCache<AccountEntity> accountCache;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * 根据账号 ID 查找账号实体
     *
     * @param accountId 账号主键（字符串）
     * @return 账号实体，不存在返回 null
     */
    public AccountEntity findById(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            return null;
        }
        return accountCache.findById(accountId);
    }

    /**
     * 查询某账号下是否存在指定角色
     *
     * @param accountId 账号 ID
     * @param roleId    角色 ID
     * @return 存在返回 true，否则 false
     */
    public boolean existsRole(String accountId, long roleId) {
        AccountEntity account = findById(accountId);
        if (account == null || account.getRoleBriefList() == null) {
            return false;
        }
        return account.getRoleBriefList().stream()
                .anyMatch(r -> r.getRoleId() == roleId);
    }

    /**
     * 获取某账号下最近一次登录的角色简要信息
     *
     * @param accountId 账号 ID
     * @return 最近登录的角色简要信息，无角色或无记录时返回 empty
     */
    public Optional<AccountRoleBrief> getLastLoginRole(String accountId) {
        AccountEntity account = findById(accountId);
        if (account == null || account.getRoleBriefList() == null || account.getRoleBriefList().isEmpty()) {
            return Optional.empty();
        }
        List<AccountRoleBrief> list = account.getRoleBriefList();
        return list.stream()
                .filter(r -> r.getLastLoginTime() != null)
                .max(Comparator.comparing(AccountRoleBrief::getLastLoginTime));
    }

    /**
     * 保存账号实体
     *
     * @param account 账号实体
     * @return 保存后的实体
     */
    public AccountEntity save(AccountEntity account) {
        account.setUpdateTime(LocalDateTime.now());
        return accountCache.save(account);
    }

    /**
     * 保存账号实体单个字段
     *
     * @param account   账号实体
     * @param fieldName 字段名
     */
    public void saveField(AccountEntity account, String fieldName) {
        accountCache.saveField(account, fieldName);
    }

    /**
     * 创建或获取账号实体；若不存在则创建并入库
     *
     * @param accountId 账号 ID
     * @return 账号实体（新建或已存在）
     */
    public AccountEntity getOrCreate(String accountId) {
        AccountEntity account = findById(accountId);
        if (account != null) {
            return account;
        }
        account = new AccountEntity();
        account.setId(accountId);
        account.setRoleBriefList(new ArrayList<>());
        return accountCache.insert(account);
    }

    /**
     * 预加载全部账号到缓存
     * 需在玩家数据加载之前调用
     */
    public void loadAll() {
        accountCache.loadAll();
    }

    /**
     * 更新某账号下某角色的最近登录时间；若该角色不存在于账号下则新增一条简要信息
     *
     * @param accountId 账号 ID
     * @param roleId    角色 ID
     * @param loginTime 登录时间
     */
    public void updateRoleLoginTime(String accountId, long roleId, LocalDateTime loginTime) {
        AccountEntity account = getOrCreate(accountId);
        List<AccountRoleBrief> list = account.getRoleBriefList();
        AccountRoleBrief existing = list.stream()
                .filter(r -> r.getRoleId() == roleId)
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setLastLoginTime(loginTime);
        } else {
            list.add(AccountRoleBrief.valueOf(roleId, loginTime));
        }
        accountCache.saveField(account, AccountEntity.Fields.roleBriefList);
    }
}
