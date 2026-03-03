package com.slg.web.account.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.web.account.entity.UserEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 角色服务（Web 侧）
 * 管理账号在各 game 服上的角色记录，UserEntity 仅由 game 侧创角/登出回调写入
 * 主键 = roleId，查询直接走 EntityCache 主键索引，O(1)
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
     * 根据 roleId 查找角色（主键查询，O(1)）
     *
     * @param roleId 角色 ID（等同于 UserEntity 主键）
     * @return 角色实体，不存在返回 null
     */
    public UserEntity findByRoleId(long roleId) {
        return userCache.findById(roleId);
    }

    /**
     * 创建新角色记录（由 game 侧创角回调驱动）
     *
     * @param roleId   game 侧生成的角色 ID，作为主键
     * @param serverId 所在 game 服 ID
     * @param accId    关联的 Account ID
     * @return 新创建的角色实体
     */
    public UserEntity createUser(long roleId, int serverId, long accId) {
        UserEntity user = new UserEntity();
        user.setId(roleId);
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
