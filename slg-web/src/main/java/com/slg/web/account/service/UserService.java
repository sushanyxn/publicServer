package com.slg.web.account.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.web.account.entity.UserEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色服务（Web 侧）
 * 管理账号在各 game 服上的角色记录
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Service
public class UserService {

    @Getter
    private static UserService instance;

    @EntityCacheInject
    private EntityCache<UserEntity> userCache;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * 全量加载角色数据到缓存
     */
    public void loadAll() {
        userCache.loadAll();
    }

    /**
     * 根据 roleId 查找角色
     */
    public UserEntity findByRoleId(long roleId) {
        return userCache.getAllCache().stream()
                .filter(u -> u.getRoleId() == roleId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 Account ID 查找所有角色
     */
    public List<UserEntity> findByAccId(long accId) {
        return userCache.getAllCache().stream()
                .filter(u -> u.getAccId() == accId)
                .toList();
    }

    /**
     * 根据 Account ID 和 serverId 查找角色
     */
    public UserEntity findByAccIdAndServerId(long accId, long serverId) {
        return userCache.getAllCache().stream()
                .filter(u -> u.getAccId() == accId && u.getServerId() == serverId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 创建新角色
     *
     * @param serverId 服务器 ID
     * @param accId    关联的 Account ID
     * @return 新创建的角色实体（roleId 在 game 侧首次登录时回写）
     */
    public UserEntity createUser(long serverId, long accId) {
        UserEntity user = new UserEntity();
        user.setServerId(serverId);
        user.setAccId(accId);
        user.setLockStatus(UserEntity.LOCK_STATUS_NORMAL);
        user.setCreateTime(LocalDateTime.now());
        return userCache.insert(user);
    }

    /**
     * 保存（更新）角色
     */
    public UserEntity save(UserEntity user) {
        user.setUpdateTime(LocalDateTime.now());
        return userCache.save(user);
    }

    /**
     * 保存单个字段
     */
    public void saveField(UserEntity user, String fieldName) {
        userCache.saveField(user, fieldName);
    }
}
